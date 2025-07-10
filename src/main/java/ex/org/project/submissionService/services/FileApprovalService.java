package ex.org.project.submissionService.services;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.opencsv.CSVWriter;
import ex.org.project.submissionService.exceptions.custom.*;
import ex.org.project.submissionService.models.*;
import ex.org.project.submissionService.models.dtos.*;

import ex.org.project.submissionService.auth.UserNotFoundException;
import ex.org.project.submissionService.emails.EmailRequestService;

import ex.org.project.submissionService.exceptions.custom.BadDataException;
import ex.org.project.submissionService.exceptions.custom.DataFileNotFoundException;
import ex.org.project.submissionService.exceptions.custom.FileDeletionException;

import ex.org.project.submissionService.models.dtos.DataSubmissionDTO;
import ex.org.project.submissionService.models.dtos.SubmissionApprovalDTO;
import ex.org.project.submissionService.emails.DataIngestEmailType;
import ex.org.project.submissionService.mappers.SubmissionByCuratorMapper;
import ex.org.project.submissionService.repositories.*;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FilenameUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ex.org.project.submissionService.mappers.DataFileMapper;
import ex.org.project.submissionService.mappers.DataSubmissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;

@Slf4j
@RequiredArgsConstructor
@Service
public class FileApprovalService {

    private final LkupStatusRepository lkupStatusRepository;
    private final DataSubmissionRepository dataSubmissionRepository;
    private final StudyRepository studyRepository;
    private final DataFileRepository dataFileRepository;
    private final DataFileService dataFileService;
    private final StorageService storageService;
    private final DataSubmissionMapper dataSubmissionMapper;
    private final DataFileMapper dataFileMapper;
	private final ViewStudyRepository viewStudyRepository;
	private final S3TransferManager transferManager;
	private final EmailRequestService emailRequestService;
	private final SubmissionByCuratorMapper submissionByCuratorMapper;
	private final UsersRepository usersRepository;
	private final DownloadService downloadService;
	private final StudyPropertyValueRepository studyPropertyValueRepository;

	@Value("${s3.download-directory}")
	private String workingDirectory;

	private static final String[] CSV_HEADER = {"Validation Type", "Error Type", "File", "File Type", "Line Number", "Column Header", "Value", "Message", "Solution"};

    public List<DataSubmissionDTO> getSubmittedSubmissions(String statusValue) {
        LkupStatus status = lkupStatusRepository.findByUsageAndName(Constants.USAGE_DATA_SUBMISSION, statusValue)
				.orElseThrow(()  -> new StatusNotFoundException(String.format("An error occurred while fetching submitted submissions. Invalid data submission status: %s", statusValue)));
        List<DataSubmission> dataSubmissions = dataSubmissionRepository.findDataSubmissionsByStatusOrderByDateSubmittedDesc(
                status);
        List<DataSubmissionDTO> submittedSubmissionsDTOs = new ArrayList<>();
		Map<Integer, String> userIdToNameMap = getUserIdToNameMap(dataSubmissions);
		Map<Integer, ViewStudy> studyIdToViewMap = getStudyIdToViewMap(dataSubmissions);

        for(DataSubmission dataSubmission : dataSubmissions) {
			int studyId = dataSubmission.getStudyId();
            if(!studyIdToViewMap.containsKey(studyId)) {
				log.error("Could not find study: {}", studyId);
				continue;
			}
			ViewStudy viewStudy = studyIdToViewMap.get(studyId);
			DataSubmissionDTO dataSubmissionDTO = dataSubmissionMapper.toDTO(viewStudy,dataSubmission);
			String submitterName = userIdToNameMap.get(dataSubmission.getSubmitterUserId());
			dataSubmissionDTO.setDCCRepresentative(submitterName);
			submittedSubmissionsDTOs.add(dataSubmissionDTO);
        }

		return submittedSubmissionsDTOs;
	}
	private Map<Integer, String> getUserIdToNameMap(List<DataSubmission> dataSubmissions) {
		List<Integer> submitterIds = dataSubmissions.stream().map(DataSubmission::getSubmitterUserId).toList();
		return usersRepository.findAllById(submitterIds).stream()
				.collect(Collectors.toMap(Users::getId, Users::getFullName));
	}

	private Map<Integer, ViewStudy> getStudyIdToViewMap(List<DataSubmission> dataSubmissions) {
		List<Integer> studyIds = dataSubmissions.stream().map(DataSubmission::getStudyId).toList();
		return viewStudyRepository.findAllById(studyIds).stream()
				.collect(Collectors.toMap(ViewStudy::getStudyId, vs -> vs));
	}
	
	/**
    This method retrieves the data files associated with a given submission ID
    @param submissionId The ID of the submission
    @return A DataFileDTO object representing the data files with versions numbers reflecting
    		values if the file is approved. 
    */
	public DetailsDTO getSubmissionBundleInfo(Integer submissionId) {
        DataSubmission dataSubmission = dataSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionIdInvalidException("Submission not found for ID: " + submissionId));
        List<DataFile> allSubmissionFiles = dataFileRepository.findBySubmissionId(submissionId);

		// Find the user associated with the data submission
		Users user = usersRepository.findById(dataSubmission.getSubmitterUserId())
                .orElseThrow(() -> new UserNotFoundException(
                        String.format("Submission ID %d contains invalid user ID", submissionId)
                ));

		List<BundlesDTO> bundlesDtos = createBundlesDtos(allSubmissionFiles);

		ViewStudy study = viewStudyRepository.findByStudyId(dataSubmission.getStudyId())
                .orElseThrow(() -> new StudyNotFoundException("Study not found for submission ID " + submissionId));

		// Map studies to DetailsDTO and set DCC representative
		DetailsDTO detailsDto = submissionByCuratorMapper.toDTOs(study);
		detailsDto.setDccRep(user.getFullName());
        detailsDto.setDcc(study.getDCC());

		// Set bundles, ID, and add to the list of DetailsDTO objects
		detailsDto.setBundles(bundlesDtos);
		detailsDto.setSubmissionId(submissionId);

		return detailsDto;
	}

    private List<BundlesDTO> createBundlesDtos(List<DataFile> allSubmissionFiles){
        // Map BundlesDTO objects from data files
        List<DataFile> topLevelDataFiles = allSubmissionFiles.stream()
                .filter(this::isTopLevelCategory)
                .toList();
		allSubmissionFiles.removeAll(topLevelDataFiles);
        Map<Integer, Map<String, Boolean>> childValidationStatuses = getChildValidationStatusMap(topLevelDataFiles,
																								 allSubmissionFiles);
		List<Integer> childFileIds = getChildFileIds(topLevelDataFiles, childValidationStatuses);
		List<DataFile> unassignedFiles = getUnassignedFiles(allSubmissionFiles, childFileIds);
		List<DataFile> hierarchicalDataFiles = Stream.of(topLevelDataFiles, unassignedFiles)
				.flatMap(Collection::stream).toList();
        List<BundlesDTO> bundlesDtos = dataFileMapper.mapToBundleDTOList(hierarchicalDataFiles);

        for(BundlesDTO bundle : bundlesDtos) {
            if(childValidationStatuses.containsKey(bundle.getId())){
				Map<String, Boolean> validations = childValidationStatuses.get(bundle.getId());
				bundle.setDictFailed(validations.get("dict"));
				bundle.setMetaFailed(validations.get("meta"));
            }
        }
        return bundlesDtos;
    }

	private boolean isTopLevelCategory(DataFile df) {
		return df.getFileCategory().getCategoryGroup().equals(Constants.CATEGORY_DATA)
				|| df.getFileCategory().getCategoryGroup().equals(Constants.CATEGORY_DOCUMENT)
				|| df.getFileCategory().getCategoryGroup().equals("other");
	}

	private Map<Integer, Map<String, Boolean>> getChildValidationStatusMap(List<DataFile> topLevelDataFiles,
																		   List<DataFile> allSubmissionFiles) {
		return topLevelDataFiles.stream()
				.filter(df -> df.getDictionaryFileId() != null || df.getMetadataFileId() != null)
				.collect(Collectors.toMap(
						DataFile::getId,
						df -> Map.of("dict", getDictFailed(df, allSubmissionFiles),
									 "meta", getMetaFailed(df, allSubmissionFiles))
										 ));
	}

    private Boolean getDictFailed(DataFile parentFile, List<DataFile> submissionFiles){
		if(parentFile.getDictionaryFileId() == null) {
			return true;
		}
        Optional<DataFile> dictFile = submissionFiles.stream()
                .filter(df -> df.getId().equals(parentFile.getDictionaryFileId()))
                .findFirst();

        if(dictFile.isPresent()){
			Boolean dictFailed = dictFile.get().getDictValidationFailed();
            return dictFailed == null ? true : dictFailed;
        }
        log.error("No dictionary file validation found");
        return true;
    }

    private Boolean getMetaFailed(DataFile parentFile, List<DataFile> submissionFiles){
		if(parentFile.getMetadataFileId() == null) {
			return true;
		}
        Optional<DataFile> metaFile = submissionFiles.stream()
                .filter(df -> df.getId().equals(parentFile.getMetadataFileId()))
                .findFirst();

        if(metaFile.isPresent()){
			Boolean metaFailed = metaFile.get().getMetaValidationFailed();
            return metaFailed == null ? true : metaFailed;
        }
        log.error("No metadata file validation found");
        return true;
    }

	private List<Integer> getChildFileIds(List<DataFile> topLevelFiles, Map<Integer, Map<String, Boolean>> childValidationStatuses) {
		return topLevelFiles.stream()
				.filter(df -> childValidationStatuses.containsKey(df.getId()))
				.flatMap(df -> Stream.of(df.getMetadataFileId(), df.getDictionaryFileId()))
				.toList();
	}

	private List<DataFile> getUnassignedFiles(List<DataFile> dataFiles, List<Integer> childFileIds) {
		return dataFiles.stream()
				.filter(df -> !childFileIds.contains(df.getId()))
				.toList();
	}

    /**
     * This method approves or rejects a file submission based on the provided status.
     * @param submissionApprovalDTO containing any file rejection reasons and a list of bundle parent files to be processed
     * @return true if successful
     */
    @Transactional
    public boolean processSubmission(SubmissionApprovalDTO submissionApprovalDTO) {

        Integer submissionId = submissionApprovalDTO.getSubmissionDetails().getSubmissionId();
        DataSubmission submission = dataSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionIdInvalidException("Submission Id " + submissionId + " not found"));
        Set<DataFile> approvedFiles = new HashSet<>();
        Set<DataFile> rejectedFiles = new HashSet<>();

		submissionApprovalDTO.getSubmissionDetails().getBundles().forEach(bundle -> {
            Integer dataFileId = bundle.getId();
            String decision = bundle.getReviewDecision();
            if (decision == null) {
                throw new BadDataException("Invalid decision parameter: null");
            }
            switch (decision.toUpperCase()) {
                case "APPROVED" -> approvedFiles.addAll(approved(dataFileId, submission.getStudyId()));
                case "REJECTED" -> rejectedFiles.addAll(rejected(dataFileId));
                default -> throw new BadDataException("Invalid decision parameter: \"" + decision + "\"");
            }
		});

		verifyFilesWereProcessed(approvedFiles, rejectedFiles);
		setApprovedStatus(approvedFiles);
        String studyUuid = studyRepository.findStudyById(submission.getStudyId()).getUuid();
		List<S3File> filesToMove = getS3FilesToMoveWithNewKey(approvedFiles, studyUuid);
		String approvedFileNames = getApprovedFileNames(approvedFiles);
		if(!filesToMove.isEmpty()) {
			storageService.moveToApproved(filesToMove);
		}
        int rejectedFileCount = rejectedFiles.size();
		String rejectedFileNames = "";
        if(!rejectedFiles.isEmpty()) {
            rejectedFileNames = deleteRejectedFiles(rejectedFiles);
        }
        submission.setFileRejectedCount(rejectedFileCount);
        submission.setFileRejectionReason(submissionApprovalDTO.getFileRejectionReason());

        // Update Submission Status and send emails
        LkupStatus submissionStatus = lkupStatusRepository.findByUsageAndName(Constants.USAGE_DATA_SUBMISSION, Constants.STATUS_COMPLETED)
				.orElseThrow(()  -> new StatusNotFoundException(String.format("Invalid Data Submission Status: %s", Constants.STATUS_COMPLETED)));
        submission.setStatusId(submissionStatus.getId());
        dataSubmissionRepository.save(submission);
		dataFileRepository.saveAll(approvedFiles);
		if(!approvedFiles.isEmpty()){
			setHasDataFilesFlag(submission.getStudyId());
		}
		String rejectionReason = submissionApprovalDTO.getFileRejectionReason() == null ? "" : submissionApprovalDTO.getFileRejectionReason();
		Map<String, String> props = Map.of("rejectedFiles", rejectedFileNames,
                                           "rejectionReason", rejectionReason,
										   "approvedFiles", approvedFileNames);
        emailRequestService.sendDataIngestEmail(submissionId, DataIngestEmailType.SUBMISSION_PROCESSED, props);
		//To populate variable counts of the approved original data file
		approvedFiles
				.forEach(
						df -> {
							if (df.getFileCategory().getName().equals(Constants.NON_HARMONIZED_DATAFILE)) {
								dataFileService.populateVariableAndSampleCount(df);
							}
						}
				);
        return true;
    }

	private String getApprovedFileNames(Set<DataFile> approvedFiles) {
		if(approvedFiles.isEmpty()){
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for(DataFile file : approvedFiles) {
			sb.append(file.getSourceFileName()).append(";");
		}
		return sb.toString();
	}

	/**
     * Method to process approved files. Retrieve DataFiles, sets fields, and returns a set of 1-3 connected approved DataFiles
     *
     * @param dataFileId ID of the DataFile to be approved
     * @param studyId ID of the submission's study
     * @return set of 0-3 connected approved DataFile objects
     */
    private Set<DataFile> approved(Integer dataFileId, Integer studyId) {
        Optional<DataFile> dfOpt = dataFileRepository.findById(dataFileId);
		if(dfOpt.isEmpty()) {
			log.error(String.format("No record found for Data File ID: %s", dataFileId));
			return new HashSet<>();
		}
		DataFile dataFile = dfOpt.get();
		Integer version = getCurrentVersionAndRemovePrevious(dataFile, studyId);
		HashSet<DataFile> approvedFiles = new HashSet<>(3);
		approvedFiles.add(dataFile);
		dataFile.setVersionNumber(version);
		dataFile.setIsCurrentVersion(true);

		List<DataFile> childFiles = dataFileRepository.findByIdIn(
				Arrays.asList(dataFile.getMetadataFileId(), dataFile.getDictionaryFileId()));
		for (DataFile file : childFiles) {
			file.setVersionNumber(version);
			approvedFiles.add(file);
		}

        return approvedFiles;
    }

	private Integer getCurrentVersionAndRemovePrevious(DataFile dataFile, Integer studyId) {
		String fileName = dataFile.getSourceFileName();
		Optional<DataFile> previousVersionOpt = dataFileRepository.findPreviousVersionByNameAndStudyId(fileName, studyId);
		if(previousVersionOpt.isEmpty()) {
			return 1;
		}
		DataFile previousVersion = previousVersionOpt.get();
		if(previousVersion.getVersionNumber() == 1){
			dataFile.setOriginalDataFile(previousVersion);
		} else {
			dataFile.setOriginalDataFile(previousVersion.getOriginalDataFile());
		}
		previousVersion.setIsCurrentVersion(false);
		dataFileRepository.save(previousVersion);
		return previousVersion.getVersionNumber() + 1;
	}

	private void verifyFilesWereProcessed(Set<DataFile> approvedFiles, Set<DataFile> rejectedFiles){
		if (approvedFiles.isEmpty() && rejectedFiles.isEmpty()) {
			String errorMessage = "No DataFiles found with provided IDs. Please check request.";
			log.warn(errorMessage);
			throw new DataFileNotFoundException(errorMessage);
		}
	}

	private void setApprovedStatus(Set<DataFile> approvedFiles) {
		LkupStatus approvedStatus = lkupStatusRepository.findByUsageAndName(Constants.USAGE_FILE, Constants.STATUS_APPROVED)
				.orElseThrow(()  -> new StatusNotFoundException(String.format("Invalid Data File Status: %s", Constants.STATUS_APPROVED)));
		for(DataFile file : approvedFiles) {
			file.setStatus(approvedStatus);
		}
	}

	private void setHasDataFilesFlag(Integer studyId) {
		//hasDataFilesPropertyValue is created and set to no at study creation,
		//update has_datafile flag once file is approved
			StudyPropertyValue studyPropertyValue = studyPropertyValueRepository
					.findByEntityProperty_NameAndStudyId(Constants.STUDY_PROP_HAS_DATAFILES, studyId);
			if(studyPropertyValue.getPropertyValue().equals("No")){
				studyPropertyValue.setPropertyValue("Yes");
				studyPropertyValueRepository.save(studyPropertyValue);
			}
	}

	private List<S3File> getS3FilesToMoveWithNewKey(Set<DataFile> dataFiles, String studyUuid) {
		List<S3File> s3Files = new ArrayList<>(dataFiles.size());
		for(DataFile dataFile : dataFiles) {
			S3File s3File = dataFile.getS3File();
			setNewS3FileKey(studyUuid, s3File, dataFile.getVersionNumber());
			s3Files.add(s3File);
		}
		return s3Files;
	}

	private void setNewS3FileKey(String studyUuid, S3File s3File, Integer version) {
		String fileName = s3File.getFileName();
		String base = FilenameUtils.getBaseName(fileName);
		String extension = FilenameUtils.getExtension(fileName);
		String newKey = String.format("%s/%s_v%d.%s", studyUuid, base, version, extension);
		s3File.setNewFileKey(newKey);
	}

    /**
     * Method to process a rejected file. Retrieves and returns the DataFile by ID.
     *
     * @param dataFileId ID of the file that is being rejected
     * @return The DataFile object that is being rejected
     */
    private Set<DataFile> rejected(Integer dataFileId) {
        Optional<DataFile> dfOpt = dataFileRepository.findById(dataFileId);
		if(dfOpt.isEmpty()) {
			log.error(String.format("No record found for Data File ID: %s", dataFileId));
			return new HashSet<>();
		}
		DataFile dataFile = dfOpt.get();
        HashSet<DataFile> rejectedFiles = new HashSet<>(3);
		rejectedFiles.add(dataFile);

		if(dataFile.getMetadataFileId() != null) {
			Optional<DataFile> dataFileMeta = dataFileRepository.findById(dataFile.getMetadataFileId());
			dataFileMeta.ifPresent(rejectedFiles::add);
		}
		if(dataFile.getDictionaryFileId() != null) {
			Optional<DataFile> dataFileDictionary = dataFileRepository.findById(dataFile.getDictionaryFileId());
			dataFileDictionary.ifPresent(rejectedFiles::add);
		}
        return rejectedFiles;
    }

    /**
     * Deletes a set of rejected Datafiles. This includes deletion from the DataFile and S3File tables, and AWS S3
     *
     * @param rejectedFiles Set of rejected Datafiles
     */
    private String deleteRejectedFiles(Set<DataFile> rejectedFiles) {
		StringBuilder sb = new StringBuilder();
        dataFileRepository.updateForeignKeysToNull(
                rejectedFiles.stream().map(DataFile::getId).collect(Collectors.toSet()));
        List<Integer> deletedSuccessfully = new ArrayList<>(rejectedFiles.size());
        for(DataFile df : rejectedFiles) {
			sb.append(df.getSourceFileName()).append(";");
            S3File s3File = df.getS3File();
            try {
                if(df.getS3File() == null) {
                    throw new DataFileNotFoundException("No S3 file found for DataFile ID " + df.getId());
                }
                s3File.setS3FileKeyAndBucketFromPath();
                //delete object from data_file and s3_file tables, and the s3 bucket
                dataFileRepository.deleteById(df.getId());
                boolean deletionSuccessful = dataFileService.deleteS3FileAndEntity(s3File);
                if(deletionSuccessful) {
                    deletedSuccessfully.add(df.getId());
                }
                else {
                    throw new FileDeletionException(
                            "Error deleting file from S3 storage services; S3File ID: " + s3File.getId());
                }
            }
            catch(DataAccessException e) {
                log.error("Error deleting file from database; FileId: " + df.getId(), e);
            }
            catch(DataFileNotFoundException | FileDeletionException e) {
                log.error(e.getMessage());
            }
        }
        if(!deletedSuccessfully.isEmpty()) {
            log.info("Successfully deleted data files: " + deletedSuccessfully);
        }
		return sb.toString();
    }


	public ResponseEntity<Object> getAllSubmissionFiles(Integer submissionId){

		//Retrieve the Submission and validate
		DataSubmission dataSubmission = dataSubmissionRepository.findById(submissionId)
				.orElseThrow(() -> new SubmissionIdInvalidException("Could not find submission ID: " + submissionId));
		String submissionStatus = dataSubmission.getStatus().getName();
		//check if status is valid
		lkupStatusRepository.findByUsageAndName(Constants.USAGE_DATA_SUBMISSION, submissionStatus)
				.orElseThrow(()  -> new StatusNotFoundException(String.format("Could not find submission status entity. Invalid Status: %s", submissionStatus)));
		if(!submissionStatus.equals(Constants.STATUS_SUBMITTED)){
			throw new StatusNotFoundException("Submission status must be Submitted.");
		}

		//Get data files and extract S3 file IDs
		List<DataFile> dataFiles = dataFileRepository.findDataFilesBySubmissionId(submissionId);

		if(dataFiles.isEmpty()){
			String emptyListWarning = "No data files found for submission id: " + submissionId;
			log.warn(emptyListWarning);
			throw new DataFileNotFoundException(emptyListWarning);
		}

		//Build the map of S3 file IDs which is used to help us put Downloads in the correct folder of the Zip file later.
		//Also build the list of S3 File IDs which will be used to retrieve the S3 Files from the DB later.
		//If at any point we find a datafile which is missing an S3 File ID, we cancel that bundle and log an error.
		Map<Integer, String> s3IdMap = new HashMap<>();
		List<DataFile> bundleFiles = new ArrayList<>();
		for (DataFile df : dataFiles){
			if (df.getS3File() != null) {
				addBundleToMapAndList(df, s3IdMap, bundleFiles);
				s3IdMap.put(df.getS3File().getId(), df.getSourceFileName());
			} else {
				log.error("Data file {} is missing S3 File", df.getSourceFileName());
			}
		}
		dataFiles.addAll(bundleFiles);

		return getDownloadListFromS3(s3IdMap, dataFiles, submissionId);
	}

	/**
	 * Helper method for returning all submission files as a ZIP. Retrieves the S3 file IDs for the Dict/Meta files
	 * related to the given Data File, and adds the required details to the given list and map. If any S3 File ID is
	 * missing, the method will return false. If the method successfully adds all IDs to the list and map, returns true.
	 */
	public void addBundleToMapAndList(DataFile df, Map<Integer, String> s3IdMap, List<DataFile> bundleFiles){
		List<Integer> bundleIds = new ArrayList<>();
		if (df.getDictionaryFileId() != null){
			bundleIds.add(df.getDictionaryFileId());
		}
		if (df.getMetadataFileId() != null){
			bundleIds.add(df.getMetadataFileId());
		}
		List<DataFile> bundleDataFiles = dataFileRepository.findByIdIn(bundleIds);

		if (!bundleDataFiles.isEmpty()){
			for (DataFile bundleFile : bundleDataFiles){
				s3IdMap.put(bundleFile.getS3File().getId(), df.getSourceFileName());
				bundleFiles.add(bundleFile);
			}
		}
	}


	/**
	 * Retrieves the files for a submission. Organizes the files from S3 into a zip file in the format
	 * where each key in the given map will be a sub-folder within the zip file containing
	 * the files whose ID's are represented by that key in the map.
	 */
	public ResponseEntity<Object> getDownloadListFromS3(Map<Integer, String> dataFileMap, List<DataFile> dataFiles, Integer submissionId){
		//TODO: auth check

		//get list of from db and s3 file keys
		List<DownloadDTO> downloads = new ArrayList<>();
		List<DuplicateDownloadMapDTO> bundleDownloadList = new ArrayList<>();

		for(DataFile df : dataFiles){
			S3File s3File = df.getS3File();
			s3File.setS3FileKeyAndBucketFromPath();
			if(s3File.getFileKey().isEmpty() || s3File.getFileBucket().isEmpty()){
				String errorMessage = "Couldn't find S3 file key for retrieval: ID="
						+ s3File.getId() + " , Path=" + s3File.getFilePath();
				log.error(errorMessage);
				throw new BadDataException(errorMessage);
			}
			//Create a new Download using the fields from the S3File
			DownloadDTO download = new DownloadDTO(s3File.getFileName(), s3File.getFileKey(), s3File.getFileBucket());
			//Add the Download to the list for downloading the files from S3 later
			downloads.add(download);
			//Associate the Download with its bundle for organization within the zip file later
			String fileName = dataFileMap.get(s3File.getId());
			String key = fileName.split("\\.")[0];
			bundleDownloadList.add(new DuplicateDownloadMapDTO(key, download));
		}

		List<DuplicateDownloadMapDTO> newBundleDownloadList = bundleDownloadList.stream().distinct().toList();

        File dir = new File(workingDirectory);
		if(!dir.exists()){
			dir.mkdirs();
		}

		Optional<ViewStudy> viewStudy = viewStudyRepository.findBySubmissionId(submissionId);
		String phs = viewStudy.map(study -> study.getPhs() + "_").orElse("");

		//create temp dir for files in request
		final String zipName = workingDirectory + phs + "BundledSubmissionFiles";
		File zipDir = new File(zipName);
		if(!zipDir.exists()){
			zipDir.mkdirs();
		}
		//initiate download from s3
		for(DownloadDTO download : downloads){
			download.setLocalFile(
					new File(zipName + "/" + download.getFileName())
			);
			DownloadFileRequest request =
					DownloadFileRequest.builder()
							.getObjectRequest(b -> b.bucket(download.getS3Bucket()).key(download.getS3Key()))
							.destination(Paths.get(download.getLocalFile().getPath()))
							.build();
			download.setFileDownload(transferManager.downloadFile(request));
		}
		//join futures into local files
		for(DownloadDTO download : downloads){
			download.getFileDownload().completionFuture().join();
		}

		String zipFileName = zipName + ".zip";
		Path zipPath = Paths.get(zipFileName);
		//Create the Zip file to be returned
		try (FileOutputStream fos = new FileOutputStream(zipFileName);
			 ZipOutputStream zos = new ZipOutputStream(fos)){

			for (DuplicateDownloadMapDTO mapValue : newBundleDownloadList) {
				//Add a new file to the ZIP in the appropriate folder based on the key
				ZipEntry zipDataFile = new ZipEntry(mapValue.getDownload().getFileName());
				zos.putNextEntry(zipDataFile);
				FileInputStream fis = new FileInputStream(mapValue.getDownload().getLocalFile().getPath());
				zos.write(fis.readAllBytes());
				zos.closeEntry();
				fis.close();
			}

			//Create the warning report for the submission
			ZipEntry zipWarningReport = new ZipEntry("WarningReport.csv");
			zos.putNextEntry(zipWarningReport);

			CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(zos));
			csvWriter.writeNext(CSV_HEADER);

			List<String[]> allLines = new ArrayList<>();

			for (DataFile df : dataFiles){
				try {
					if (df.getValidationResults() != null) {
						// Parse the validation results from JSON to ValidationResult object
						ValidationResult validationResult = downloadService.getDataFileErrors(df);
						// Generate a CSV report and add rows
						downloadService.addValidationErrorsToCSV(validationResult, allLines);
					}
				} catch (IOException e) {
					log.error("Error creating submission Waring Report CSV.", e);
					try {
						Files.delete(zipPath);
						FileUtils.deleteDirectory(zipDir);
					} catch (IOException deletionError){
						log.error("Error deleting zip file from working directory", deletionError);
						throw new FileReadWriteException("Problem creating zip file; Directory deletion error");
					}
					throw new FileReadWriteException("Error creating submission Waring Report CSV.");
				}
			}
			if(!allLines.isEmpty()){
				csvWriter.writeAll(allLines);
			}
			csvWriter.close();
		} catch (IOException e){
			log.error("Error writing files to zip file", e);
			try {
				Files.delete(zipPath);
				FileUtils.deleteDirectory(zipDir);
			} catch (IOException deletionError){
				log.error("Error deleting zip file from working directory", deletionError);
				throw new FileReadWriteException("Problem creating zip file; Directory deletion error");
			}
			throw new FileReadWriteException("Problem creating zip file, please try again");
		}

		//Finalize the response object, add body and headers, return. Don't forget to close any file objects remaining.
		try {
			FileInputStream fileStream = new FileInputStream(zipFileName);
			byte[] responseFile = fileStream.readAllBytes();
			fileStream.close();
			HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFileName.split(workingDirectory)[1]);
			ResponseEntity<Object> response = ResponseEntity.ok()
					.headers(headers)
					.contentType(MediaType.APPLICATION_OCTET_STREAM)
					.body(responseFile);
			Files.delete(zipPath);
			FileUtils.deleteDirectory(zipDir);
			return response;
		} catch (IOException e){
			log.error("Error writing files to Response Entity", e);
			try {
				FileUtils.deleteDirectory(zipDir);
			} catch (IOException deletionError){
				log.error("Error deleting zip file from working directory", deletionError);
				throw new FileReadWriteException("Error reading file; Directory deletion error");
			}
			throw new FileReadWriteException("Error reading file");
		}
	}

}

