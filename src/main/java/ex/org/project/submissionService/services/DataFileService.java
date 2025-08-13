package ex.org.project.submissionService.services;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import com.amazonaws.services.macie2.model.CreateClassificationJobResult;
import com.opencsv.*;
import ex.org.project.submissionService.auth.UserNotFoundException;
import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.CSVReaderHeaderAwareBuilder;

import ex.org.project.submissionService.exceptions.custom.*;
import ex.org.project.submissionService.mappers.S3FileMapper;
import ex.org.project.submissionService.models.*;
import ex.org.project.submissionService.models.DataFileDownload;
import ex.org.project.submissionService.models.dtos.S3FileDTO;
import ex.org.project.submissionService.models.dtos.StudiesDTO;
import ex.org.project.submissionService.models.dtos.UploadFilesDTO;
import ex.org.project.submissionService.models.dtos.ValidationResultsDTO;
import ex.org.project.submissionService.repositories.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import ex.org.project.submissionService.mappers.StudyMapper;
import ex.org.project.submissionService.mappers.UploadMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static ex.org.project.submissionService.models.Constants.STUDY_PROP_HAS_DATAFILES;

@Slf4j
@RequiredArgsConstructor
@Service
public class DataFileService {

    private final S3FileRepository s3FileRepository;
    private final DataFileRepository dataFileRepository;
    private final DataFileCategoryRepository dataFileCategoryRepository;
    private final LkupStatusRepository lkupStatusRepository;
    private final AwsStorageService storageService;
    private final ValidationService validationService;
    private final DataSubmissionRepository dataSubmissionRepository;
    private final StudyPropertyValueRepository studyPropertyValueRepository;
    private final StudyRepository studyRepository;
    private final UsersRepository usersRepository;
    private final DataFileVariableRepository dataFileVariableRepository;
    private final SASFileRepository sasFileRepository;
    private final SASFileDownloadRepository sasFileDownloadRepository;
    private final DataFileDownloadRepository dataFileDownloadRepository;
    private final StudyMapper studiesMapper;
    private final S3FileMapper s3FileMapper;

    /**
     * Creates a DataFile entity based on an S3File Object
     * @param s3File the object on which to base the DataFile entry
     * @param userId ID of the user
     * @return the newly created and saved DataFile
     */

    @Transactional
    public DataFile createDataFile(S3File s3File, Integer userId)  {
        String fileCategoryName = processFileCategory(s3File);
        // Find the DataFileCategory by its name
        DataFileCategory fileCategory = dataFileCategoryRepository.findByName(fileCategoryName);
        // Find the LkupStatus by its usage and name
        LkupStatus status = lkupStatusRepository.findByUsageAndName(Constants.USAGE_FILE, Constants.STATUS_DRAFT)
                .orElseThrow(()  -> new StatusNotFoundException(String.format("Invalid Data File Status: %s", Constants.STATUS_DRAFT)));
        // Create a new DataFile object
        DataFile dataFile = new DataFile(s3File, s3File.getSubmissionId(), fileCategory, normalizeFileName(s3File), status);
        dataFile.setCreatedBy(userId);
        return dataFileRepository.saveAndFlush(dataFile);
    }

    /**
     * Uploads the provided files to S3, categorizes them, and saves each entry to the database
     * @param files A list of files to be uploaded to S3 and stored as DataFiles
     * @param submissionId
     * @param userId ID of the user
     * @return data about the file that was uploaded and its upload success status
     */
    public List<S3FileDTO> createDataFiles(List<MultipartFile> files, Integer submissionId, Integer userId){
        if (files.isEmpty()) {
            throw new EmptyParameterException("Files parameter is empty");
        }
        //check if data submission is valid
        dataSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionIdInvalidException("Submission ID parameter is invalid. Could not find submission ID: " + submissionId));

        Study study = studyRepository.findByDataSubmission_Id(submissionId);
        if(study == null){
            throw new StudyNotFoundException("No study found for submission ID: " + submissionId);
        }
        String uuid = study.getUuid();

        //check is user id is valid
        usersRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("No user found for user ID " + userId));
        List<S3File> s3Files = files.stream().parallel()
                .map(file -> storageService.uploadFile(file, submissionId, uuid, userId))
                .toList();

        log.info("Files uploaded to S3: " + s3Files.stream()
                .filter(S3File::getUploadSuccessful)
                .map(S3File::getFileName).toList()
        );

        Map<S3File, S3FileDTO> s3FileDTOMap = s3Files.stream()
                .collect(Collectors.toMap(
                        s3File -> s3File,
                        s3FileMapper::s3FileToDTO
                ));

        s3FileDTOMap.entrySet().stream()
                .filter(entry -> entry.getValue().getUploadSuccessful())
                .forEach(entry -> {
                    DataFile dataFileId = createDataFile(entry.getKey(), userId);
                    entry.getValue().setDataFileId(dataFileId.getId());
                });

        return s3FileDTOMap.values().stream().toList();

    }

    /**
     * Deletes a DataFile, including any foreign key references and its corresponding S3File entity and file
     * @param fileId the id of the DataFile that is to be deleted
     * @return true if the file was successfully deleted, false otherwise
     */
    public boolean deleteDataFile(Integer fileId){

        //get datafile object from db and it's respective s3 file object
        Optional<DataFile> dataFileOptional = dataFileRepository.findById(fileId);
        if(dataFileOptional.isEmpty()){
            throw new DataFileNotFoundException("No file found with ID " + fileId);
        }
        DataFile dataFile = dataFileOptional.get();
        S3File s3File = dataFile.getS3File();
        if(dataFile.getS3File() == null){
            throw new DataFileNotFoundException("No S3 file found for ID " + fileId);
        }
        s3File.setS3FileKeyAndBucketFromPath();

        //check if any data files have file to be deleted as a foreign key reference
        //if so, get them and set reference to null
        List<DataFile> foreignKeyDictFiles = dataFileRepository.findDataFilesByDictionaryFileId(fileId);
        if (!foreignKeyDictFiles.isEmpty()) {
            foreignKeyDictFiles.forEach(df -> df.setDictionaryFileId(null));
            dataFileRepository.saveAll(foreignKeyDictFiles);
        }

        List<DataFile> foreignKeyMetaFiles = dataFileRepository.findDataFilesByMetadataFileId(fileId);
        if (!foreignKeyMetaFiles.isEmpty()) {
            foreignKeyMetaFiles.forEach(df -> df.setMetadataFileId(null));
            dataFileRepository.saveAll(foreignKeyMetaFiles);
        }

        List<DataFileVariable> foreignKeyDataFileVariables = dataFileVariableRepository.findByDataFileId(fileId);
        if(!foreignKeyDataFileVariables.isEmpty()){
            dataFileVariableRepository.deleteAll(foreignKeyDataFileVariables);
            log.info("Deleted data file variables records for data file id: " + fileId);
        }
        //check if any download_data table has reference to current file as a file downloaded
        //if so, delete reference
        List<DataFileDownload> foreignKeyDataFilesDownloads = dataFileDownloadRepository.findDataFileDownloadsByDataFileId(fileId);
        if(!foreignKeyDataFilesDownloads.isEmpty()){
            dataFileDownloadRepository.deleteAll(foreignKeyDataFilesDownloads);
            log.info("Deleted download records for data file id: {}: \n {} ", fileId, StringUtils.join(foreignKeyDataFilesDownloads, "/n"));
        }

        //check if any sas file have reference to current file as parent data file
        //if so, delete them from sas_file table, sas_download table, s3 table and s3.
        List<SASFile> foreignKeySasFiles = sasFileRepository.findSASFilesByDataFileId(fileId);
        if (!foreignKeySasFiles.isEmpty()) {
            foreignKeySasFiles.forEach(sasFile -> {
                //delete all reference of sas files in download table
                List<SASFileDownload> sasFileDownloads = sasFileDownloadRepository.findSASFileDownloadBySasFileId(sasFile.getId());
                if(!sasFileDownloads.isEmpty()){
                    sasFileDownloadRepository.deleteAll(sasFileDownloads);
                    log.info("Deleted download records for sas file: {}: \n {} ", sasFile.getFileName(), StringUtils.join(sasFileDownloads, "/n"));
                }
                //delete all reference of sas files in s3 table and sas file from s3
                deleteS3FileAndEntity(sasFile.getS3File());
            });
            //delete all entries from sas file table
            sasFileRepository.deleteAll(foreignKeySasFiles);
            log.info("Deleted sas file records for data file id: {}: \n {} ", fileId, StringUtils.join(foreignKeySasFiles, "/n"));
        }

        //check if any data files have reference to current file as original file
        //if so, get them and set reference to null
        List<DataFile> versionedFiles = dataFileRepository.findDataFilesByOriginalDataFile_Id(fileId);
        if(!versionedFiles.isEmpty()){
            versionedFiles.forEach(df -> df.setOriginalDataFile(null));
            dataFileRepository.saveAll(versionedFiles);
            }

        try {
            //delete object from data_file and s3_file tables, and the s3 bucket
            dataFileRepository.deleteById(fileId);
            log.info("Deleted record for data file: {} ", fileId);

            boolean deletionSuccessful = deleteS3FileAndEntity(s3File);
            if (!deletionSuccessful) {
                throw new FileDeletionException("Error deleting file from S3 storage services");
            }
            return deletionSuccessful;
        } catch(DataAccessException e){
            log.error("Error writing to database; FileId: " + fileId, e);
            throw new FileDeletionException("Error deleting file from database");
        }
    }

    /**
     * Deletes multiple DataFiles, including any foreign key references and its corresponding S3File entity and file
     * @param fileIds is the list of ids of the DataFiles that are to be deleted
     */
    public boolean deleteMultipleDatafiles(List <Integer> fileIds){
        List<DataFile> dataFilesToDelete = dataFileRepository.findAllById(fileIds);
        boolean result = dataFilesToDelete.stream().allMatch(dataFile -> deleteDataFile(dataFile.getId()));
        log.info("Files deleted: " + fileIds);
        return result;
    }

    /**
     * Takes in a file id and deletes all files from the associated bundle.
     * Includes deletion of all database entities and files in S3.
     * @param fileId DataFile ID of a member of the bundle to be deleted
     * @return a list of the DataFile ID's that were deleted
     */
    @Transactional
    public List<Integer> deleteBundle(Integer fileId){
        //TODO Auth check on if user has permission to delete these files
        Set<Integer> fileIdsToDelete = getBundleIds(fileId);

        dataFileRepository.updateForeignKeysToNull(fileIdsToDelete);
        List<DataFile> bundleFiles = dataFileRepository.findAllById(fileIdsToDelete);

        List<Integer> deletedSuccessfully = new ArrayList<>(fileIdsToDelete.size());
        for(DataFile df : bundleFiles){
            S3File s3File = df.getS3File();
            try {
                if(df.getS3File() == null){
                    throw new DataFileNotFoundException("No S3 file found for DataFile ID " + df.getId());
                }
                s3File.setS3FileKeyAndBucketFromPath();
                //delete object from data_file and s3_file tables, and the s3 bucket
                dataFileRepository.deleteById(df.getId());
                boolean deletionSuccessful = deleteS3FileAndEntity(s3File);
                if (deletionSuccessful) {
                    deletedSuccessfully.add(df.getId());
                } else {
                    throw new FileDeletionException("Error deleting file from S3 storage services; S3File ID: " + s3File.getId());
                }
            } catch(DataAccessException e){
                log.error("Error deleting file from database; FileId: " + df.getId(), e);
            } catch (DataFileNotFoundException | FileDeletionException e){
                log.error(e.getMessage());
            }
        }
        return deletedSuccessfully;
    }

    public void deleteFilesAndSubmissions(Integer studyId){
        // Find the study by its ID
        studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyNotFoundException("Study not found for study ID " + studyId));
        //get all data submissions
        List<DataSubmission> studyDataSubmissions = dataSubmissionRepository.findDistinctByStudyId(studyId);

        //if the study is not approved, there are no submissions
        //if study has submissions, delete files and submissions
        if(!studyDataSubmissions.isEmpty()){
            //for each associated data submission, delete the files
            studyDataSubmissions.stream().forEach(dataSubmission -> {
                List <Integer> fileIds = dataFileRepository.findDataFilesBySubmissionId(dataSubmission.getId())
                        .stream().map(DataFile::getId)
                        .collect(Collectors.toList());
                boolean allFilesDeleted = deleteMultipleDatafiles(fileIds);
                if(allFilesDeleted){
                    //update has_datafile flag
                    StudyPropertyValue hasDataFiles = studyPropertyValueRepository.findByEntityProperty_NameAndStudyId(STUDY_PROP_HAS_DATAFILES, studyId);
                    if(hasDataFiles!=null){
                        hasDataFiles.setPropertyValue(StringUtils.capitalize(BooleanUtils.toStringYesNo(!allFilesDeleted)));
                        studyPropertyValueRepository.save(hasDataFiles);

                    }
                    //delete the submission
                    dataSubmissionRepository.delete(dataSubmission);
                }
            });
        }
    }

    /**
     * Return a set of all DataFile IDs in a Bundle, given the ID of a member of the bundle
     * @param dataFileId DataFile ID of a Bundle file
     * @return All DataFile IDs in a Bundle
     */
    public Set<Integer> getBundleIds(Integer dataFileId){
        Optional<DataFileIds> dfIdsOpt = dataFileRepository.findDistinctById(dataFileId);
        if(dfIdsOpt.isEmpty()){
            throw new DataFileNotFoundException("No DataFile found with provided id: " + dataFileId);
        }
        DataFileIds dataFile = dfIdsOpt.get();
        Set<Integer> relatedFiles = new HashSet<>(3);
        relatedFiles.addAll(extractSetOfFileIds(dataFile));

        List<DataFileIds> foreignFiles = dataFileRepository
                .findDataFileByIdInOrDictionaryFileIdInOrMetadataFileIdIn(relatedFiles, relatedFiles, relatedFiles);
        foreignFiles.forEach(df -> relatedFiles.addAll(extractSetOfFileIds(df)));
        return relatedFiles;
    }

    /**
     * Helper method to return a set of DataFile ID's associated with a specific DataFile
     * @param dfIds DataFile projection containing an id and potentially a dictionary and/or metadata file id
     * @return A set of 1-3 DataFile ID's
     */
    private Set<Integer> extractSetOfFileIds(DataFileIds dfIds){
        Set<Integer> ids = new HashSet<>(3);
        if (dfIds.getDictionaryFileId() != null){
            ids.add(dfIds.getDictionaryFileId());
        }
        if (dfIds.getMetadataFileId() != null){
            ids.add(dfIds.getMetadataFileId());
        }
        ids.add(dfIds.getId());
        return ids;
    }

    /**
     * Deletes S3 File from AWS and its corresponding S3File object from the database
     * @param s3File S3File object to be deleted
     * @return true if successful, false otherwise
     */
    public boolean deleteS3FileAndEntity(S3File s3File){
        boolean deletionSuccessful = storageService.deleteFileFromS3(s3File);
        if(deletionSuccessful){
            s3FileRepository.deleteById(s3File.getId());
            log.info("Deleted record for s3 file: {} ", s3File.getId());

        }
        return deletionSuccessful;
    }

    private String getFileFlag(S3File s3File) {
        String fname = s3File.getFileName();
        String fileExt = FilenameUtils.getExtension(fname);
        String flag = "";

        boolean isTransformFile = StringUtils.containsIgnoreCase(fname, "transformcopy");
        boolean isOrigFile = StringUtils.containsIgnoreCase(fname, "origcopy");
        boolean isRawFile = StringUtils.containsIgnoreCase(fname, "rawcopy");

        if (StringUtils.equals(fileExt, "csv")) {
            //add base flag, csv if either data or dict file
            if (StringUtils.containsIgnoreCase(fname, "_data_")) {
                flag = "Data";
            } else if (StringUtils.containsIgnoreCase(fname, "_dict_")) {
                flag = "Dict";
            }
        } else if (StringUtils.equals(fileExt, "json")) {
            //check if it is meta file
            if(StringUtils.containsIgnoreCase(fname, "_meta_")){
                flag = "Meta";
            }
        }else if (StringUtils.equals(fileExt, "sas")) {
            flag = "Sas";
        }else if (StringUtils.equals(fileExt, "fq") || StringUtils.equals(fileExt, "fastq")) {
            flag = "Seq";
        }

        //datafile, dictionary, meta and seq flags (sas??)
            char tflag = 't';
            char oflag = 'o';
            char rflag = 'r';

            //add file descriptor tag for data, dict or meta files
            if (isTransformFile) {
                flag = tflag + flag;
            } else if (isOrigFile) {
                flag = oflag + flag;
            } else if (isRawFile) {
                flag = rflag + flag;
            }
            return flag;
        }


    /**
     * This function categorizes files based on the file's name.
     *
     * @param s3File is the uploaded S3 file being categorized
     * @return string representing the file's category
     */
    private String processFileCategory(S3File s3File) {
        String filename = s3File.getFileName();

        log.debug("ABOUT TO PROCESS FILE CATEGORY FOR : " + filename);
        Tika tika = new Tika();
        String mimeType = tika.detect(filename);
        log.debug(mimeType);
        boolean isDocument = (mimeType.contains("text") || mimeType.contains("document") || mimeType.contains("msword") || mimeType.contains("pdf"))
                && !mimeType.contains("csv");

        if (mimeType.contains("image")) {
            return "Image Data";
        }
        else if (isDocument) {
            if (StringUtils.containsIgnoreCase(filename, "readme")) {
                return "README";
            } else if (StringUtils.containsIgnoreCase(filename, "_protocol")) {
                return "Study Protocol";
            }else if(StringUtils.containsIgnoreCase(filename, "_docs")){
                return "Study Documentation";
            } else if (StringUtils.containsIgnoreCase(filename, "_eligibility")) {
                return "Eligibility Criteria";
            }
        }

        String flag = getFileFlag(s3File);
        log.debug("FILE FLAG FOUND: " + flag);

        return switch (flag) {
            case "rData" -> "Tabular Data - Raw";
            case "oData" -> Constants.NON_HARMONIZED_DATAFILE;
            case "tData" -> Constants.HARMONIZED_DATAFILE;
            case "oMeta" -> Constants.NON_HARMONIZED_METAFILE;
            case "tMeta" -> Constants.HARMONIZED_METAFILE;
            case "oDict" -> Constants.NON_HARMONIZED_DICTFILE;
            case "tDict" -> Constants.HARMONIZED_DICTFILE;
            case "oSeq" -> "Sequence Data - Non-harmonized";
            case "tSeq" -> "Sequence Data - Harmonized";
            case "oSas" -> "SAS Data - Non-harmonized";
            case "tSas" -> "SAS Data - Harmonized";
            default -> "Uncategorized";
        };

    }

    private String normalizeFileName(S3File s3File) {
        String fName = FilenameUtils.removeExtension(s3File.getFileName());
        String flag = getFileFlag(s3File);

        String dataTypeFlag = switch (flag) {
            case "oData", "tData" -> "_DATA_";
            case "oDict", "tDict" -> "_DICT_";
            case "tMeta", "oMeta" -> "_META_";
            default -> "";
        };
        // current implementation is case sensitive
        return StringUtils.remove(fName, dataTypeFlag);
    }

    /**
     This method retrieves the data files and study information associated with a given submission ID
     @param submissionId The ID of the submission
     @return The UpdateDTO object containing the mapped S3FileDTOs and StudiesDTO
     */
	public UploadFilesDTO getUploadedFiles(Integer submissionId) {
        DataSubmission dataSubmission = dataSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionIdInvalidException("Could not find submission ID: " + submissionId));

		List<DataFile> dataFiles = dataFileRepository.findBySubmissionId(submissionId);
		Integer studyId = dataSubmission.getStudyId();

		StudyPropertyValue studyName = studyPropertyValueRepository.findByEntityProperty_NameAndEntityProperty_LkupEntityType_NameAndStudyId("title", "study", studyId);

		// Map the dataFiles to S3FileDTO objects and set the dataFileId
		List<S3FileDTO> s3FileDtos = dataFiles.stream().map(dataFile -> {
			S3FileDTO s3FileDto = UploadMapper.INSTANCE.toS3FileDto(dataFile);
			s3FileDto.setDataFileId(dataFile.getId()); // Set the dataFileId in the S3FileDTO
            s3FileDto.setFileSize(dataFile.getFileSize());
			return s3FileDto;
		}).collect(Collectors.toList());

		// Map StudyPropertyValue to StudiesDTO
		StudiesDTO studiesDto = studiesMapper.toDTO(studyName);
        String phs = dataSubmission.getStudy().getPhs();
        studiesDto.setDcc("(" + phs + ") " + studiesDto.getDcc());

		// Create and return the UpdateDTO with the mapped values
		return new UploadFilesDTO(s3FileDtos, studiesDto);
	}

    /**
     * Replaces the file associated with a DataFile with the provided file
     * @param fileId ID of the DataFile that is to be replaced
     * @param file new file that will be replacing the old file
     * @param userId ID of the user
     * @return validation results for the entire submission
     */
    @Transactional
    public ValidationResultsDTO replaceDataFile(Integer fileId, MultipartFile file, Integer userId){
        Optional<DataFile> dataFileOpt = dataFileRepository.findById(fileId);
        if(dataFileOpt.isEmpty()) {
            throw new DataFileNotFoundException(
                    "Could not retrieve data file to be replace. Please check file ID."
            );
        }
        //delete old s3 file
        DataFile dataFile = dataFileOpt.get();
        S3File oldS3File = dataFile.getS3File();
        DataFileCategory oldFileCategory = dataFile.getFileCategory();
        dataFile.setS3File(null); //foreign key requirement
        dataFileRepository.saveAndFlush(dataFile);
        deleteS3FileAndEntity(oldS3File);

        //upload new s3 file
        S3File newS3File = storageService.uploadFile(file, dataFile.getSubmissionId(), userId);
        LkupStatus status = lkupStatusRepository.findByUsageAndName(Constants.USAGE_FILE, Constants.STATUS_DRAFT)
                .orElseThrow(()  -> new StatusNotFoundException(String.format("Invalid Data File Status: %s", Constants.STATUS_DRAFT)));
        dataFile.updateS3File(newS3File, oldFileCategory, normalizeFileName(newS3File), status);
        dataFile = dataFileRepository.saveAndFlush(dataFile);

        //run validation on new file
        //only run PII scan on data files
//        CreateClassificationJobResult piiResults = null;
//        if (oldFileCategory.getCategoryGroup().equals(Constants.CATEGORY_DATA)) {
//            piiResults = validationService.startPIIValidationJob(dataFile, false);
//        }
        validationService.validateFile(dataFile, oldFileCategory.getName());
//        validationService.checkPIIValidationCompleted(piiResults, dataFile, dataFile.getSubmissionId(), false);
        return validationService.getFileValidationResults(dataFile.getId());
    }

    /**
     * populateVariableAndSampleCount parses through file and collects variable count, sample size and variable list
     * This function is only called for orig files at the file approval step since only transform files are parsed during validation
     * and relevant variable information is only needed from approved files.
     * @param dataFile
     */
    public void populateVariableAndSampleCount(DataFile dataFile) {
        S3File s3File = dataFile.getS3File();
        if (s3File == null) {
            throw new DataFileNotFoundException("No S3 file found for ID " + dataFile.getId());
        }
        InputStream in = storageService.getS3FileContent(s3File);
        try (var br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder().build();

            CSVReaderHeaderAware csvReader = new CSVReaderHeaderAwareBuilder(br)
                    .withCSVParser(rfc4180Parser)
                    .build();

            List <String> headers = csvReader.readMap().keySet().stream().toList();
            csvReader.readAll();
            //set sample size
            int sampleSize = (int) csvReader.getRecordsRead()-1;
            dataFile.setSampleSize(sampleSize);
            //set headers
            headers.stream().filter(s -> !s.isEmpty());
            if (!headers.isEmpty()) {
                String fileHeader = String.join(";", headers);
                //set variables list
                dataFile.setFileHeaders(fileHeader);
            }
            //set variable count
            dataFile.setVariablesCount(headers.size());
            dataFileRepository.saveAndFlush(dataFile);
        } catch (Exception e) {
            log.debug("Unable to parse file contents for datafile " + dataFile.getId()+ ": " + e.getMessage());
            throw new BadDataException("Unable to parse file contents for datafile " + dataFile.getId()+ ": " + e.getMessage());
        }
    }

}
