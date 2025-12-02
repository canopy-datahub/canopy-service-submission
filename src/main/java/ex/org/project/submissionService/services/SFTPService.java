package ex.org.project.submissionService.services;

import ex.org.project.submissionService.emails.EmailRequestService;
import ex.org.project.submissionService.emails.SftpEmailType;
import ex.org.project.submissionService.exceptions.custom.BadDataException;
import ex.org.project.submissionService.exceptions.custom.StatusNotFoundException;
import ex.org.project.submissionService.exceptions.custom.ValidationErrorException;
import ex.org.project.submissionService.models.*;
import ex.org.project.submissionService.models.dtos.SubmissionBundlesDTO;
import ex.org.project.submissionService.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
@Service
public class SFTPService {
    private final S3AsyncClient client;
    private final S3FileRepository fileRepository;
    private final S3FileTypeRepository typeRepository;
    private final DataSubmissionRepository dataSubmissionRepository;
    private final LkupSubmissionStepRepository lkupSubmissionStepRepository;
    private final LkupStatusRepository lkupStatusRepository;
    private final StudyRepository studyRepository;
    private final UsersRepository usersRepository;
    private final S3TransferManager transferManager;
    private final String s3InitialUploadBucket;
    private final String sftpQueue;
    private final SubmissionService submissionService;
    private final AwsStorageService awsStorageService;
    private final BundleService bundleService;
    private final ValidationService validationService;
    private final DataFileService dataFileService;
    private final ViewStudyRepository viewStudyRepository;
    private final EmailRequestService emailRequestService;

    private final SqsAsyncClient sqsAsyncClient;

    @Autowired
    public SFTPService(S3AsyncClient client,
                       SqsAsyncClient sqsAsyncClient,
                       S3FileRepository fileRepository,
                       S3FileTypeRepository typeRepository,
                       LkupStatusRepository lkupStatusRepository,
                       DataSubmissionRepository dataSubmissionRepository,
                       LkupSubmissionStepRepository lkupSubmissionStepRepository,
                       S3TransferManager transferManager,
                       StudyRepository studyRepository,
                       UsersRepository usersRepository,
                       @Value("${s3-bucket.in-review}") String s3InitialUploadBucket,
                       @Value("${SFTPQueue}") String sftpQueue,
                       SubmissionService submissionService,
                       AwsStorageService awsStorageService,
                       BundleService bundleService,
                       ValidationService validationService,
                       DataFileService dataFileService,
                       ViewStudyRepository viewStudyRepository,
                       EmailRequestService emailRequestService) {
        this.client = client;
        this.sqsAsyncClient = sqsAsyncClient;
        this.lkupStatusRepository = lkupStatusRepository;
        this.fileRepository = fileRepository;
        this.typeRepository = typeRepository;
        this.dataSubmissionRepository = dataSubmissionRepository;
        this.studyRepository = studyRepository;
        this.lkupSubmissionStepRepository = lkupSubmissionStepRepository;
        this.transferManager = transferManager;
        this.usersRepository = usersRepository;
        this.s3InitialUploadBucket = s3InitialUploadBucket;
        this.sftpQueue = sftpQueue;
        this.submissionService = submissionService;
        this.awsStorageService = awsStorageService;
        this.bundleService = bundleService;
        this.validationService = validationService;
        this.dataFileService = dataFileService;
        this.viewStudyRepository = viewStudyRepository;
        this.emailRequestService = emailRequestService;
    }

    @Transactional
    /**
     * processSFTPUpload
     *  - consumes and deletes the sftp message
     *  - handles the upload for each study folder in the zipfile
     *  - for each successive upload, it handles the subsequent bundling and validation of the files where applicable
     *
     * @param message - The body of the sqs message received that contains the zipfile name and download path
     * @param receiptHandle - The receipt handle of the message consumed
     * @return true if sftp process completed successfully
     */
    public boolean processSFTPUpload(String message, String receiptHandle) {
        log.info("SQS Listener Triggered for: ..." + sftpQueue);
        log.info("Messaged received to trigger sftp upload + processing : ..." + message);

        //parse relevant details from sqs message
        JSONObject json = getMessageJSON(message);
        String region = json.get("region").toString();
        String bucket = json.getJSONObject("detail").getJSONObject("bucket").get("name").toString();
        String zipfile = json.getJSONObject("detail").getJSONObject("object").get("key").toString();

        //trigger sftp file upload and store submissionIds of successful uploads
        deleteQueueMessage(sftpQueue, receiptHandle);
        SftpUploadInfo sftpUploadInfo = new SftpUploadInfo();
        HashSet<Integer> sftpSubmissions = uploadFilesSFTP(zipfile, bucket, region, sftpUploadInfo);
        //delete sftp package after successful upload
        awsStorageService.deleteSFTPPackageFromS3(zipfile, bucket, region);
        List<String> studiesUploads = viewStudyRepository.findStudyIdBySubmissionIds(sftpSubmissions);
        sftpUploadInfo.setStudies(studiesUploads);
        if (sftpSubmissions == null || sftpSubmissions.isEmpty()) {
            log.info("No valid submissions were able to be uploaded via sftp.");
            return false;
        }
        Map<String, String> props = new HashMap<>();
        String studyIds = String.join(";", sftpUploadInfo.getStudies());
        props.put("studyIds", studyIds);
         emailRequestService.sendSftpEmail(SftpEmailType.SFTP_PROCESSED, sftpUploadInfo.getUser(), props);
        //trigger default bundling for each submission
        try {
            for (Integer submissionId : sftpSubmissions) {
                log.info("current submissionId being processed : " + submissionId);

                //create bundles and update UI step from upload --> bundle step.
                boolean bundlesCreated = bundleService.createBundles(submissionId);
                String stepdescription = "Upload Files";
                Integer userId = sftpUploadInfo.getUser().getId();
                bundleService.updateStepId(submissionId, stepdescription, userId);

                if (bundlesCreated) {
                    log.info("bundles created for submission: " + submissionId);
                    //get the default bundles created in the submission
                    SubmissionBundlesDTO sftpBundles = bundleService.getBundles(submissionId);
                    //if there are no unassigned bundles, trigger validation
                    if (sftpBundles.getUnassigned() == null || sftpBundles.getUnassigned().isEmpty()) {
                        try {
                            //update UI step from bundle --> validate step and validate files in submission
                            String bundleStepdescription = "Bundle Files";
                            bundleService.updateStepId(sftpBundles.getSubmissionId(), bundleStepdescription, userId);

                             boolean validated = validationService.validateFiles(submissionId);
                             if (validated) {
                                 //update submission validated to true
                                 validationService.updateSubmissionIsValidated(submissionId, true);
                             }
                        } catch (ValidationErrorException | IllegalArgumentException e) {
                            log.info("An error occurred while validating files for sftp processing: " + e.getMessage());
                            return false;
                        } catch (Exception e) {
                            log.info("An error occurred while validating files for sftp processing: " + e.getMessage());
                            return false;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.info("Error occurred when attempting to create default bundles during sftp processing: " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * processStudyFolder loads the metadata yaml containing the study and upload information
     * and upload the files within study folder
     *
     * @param zipFileKey       - The string representing path to the zipfile uploaded via sftp
     *                         should follow path structure:  env/sftpUserId/zipFolderName.zip
     * @param sftpIngestBucket - The bucket name of where dftp uploads are made
     * @param region           is the region where the bucket is uploaded
     * @return set of study: files uploaded
     */
    private HashSet<Integer> uploadFilesSFTP(String zipFileKey, String sftpIngestBucket, String region, SftpUploadInfo sftpUploadInfo) {
        if (!StringUtils.endsWith(zipFileKey, ".zip")) {
            log.info("Unable to process non zip file: " + zipFileKey);
            return null;
        }
        S3AsyncClient s3 = S3AsyncClient.builder()
                .region(Region.of(region))
                .build();
        log.info("sftp zip file path : ..." + zipFileKey);

        String folderName = StringUtils.substringAfterLast(zipFileKey, "/");

        String sftpPath = StringUtils.substringBeforeLast(zipFileKey, "/");
        System.out.println("sftpPath: " + sftpPath);
        Users user = usersRepository.findUsersBySftpPathEqualsIgnoreCase(sftpPath).get();
        if(sftpUploadInfo.getUser() == null) {
            sftpUploadInfo.setUser(user);
        }
        HashSet<Integer> sftpSubmissionIds = new HashSet<>();
        Map<String, List<String>> studyFilesUploaded = new HashMap<>();
        try {
            //get zipfile from s3
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(sftpIngestBucket) //need to sftp bucket and sftp region to the config file
                    .key(zipFileKey)
                    .build();
            String downloadFolderPath = "src/main/resources/";

            //download zipfile
            File downloadedZipFile = new File(downloadFolderPath + folderName);
            FileUtils.copyInputStreamToFile(s3.getObject(getObjectRequest, AsyncResponseTransformer.toBlockingInputStream()).get(), downloadedZipFile);
            //create zipfile object
            ZipFile zipFile = new ZipFile(downloadedZipFile);
            log.info("zip file found: " + zipFile.getName());

            //if the zipfile is not empty, find the study folders and upload the files
            zipFile.stream().filter(zipEntry -> zipEntry.getName().endsWith("metadata.yaml") && !StringUtils.containsIgnoreCase(zipEntry.getName(),"macosx")).forEach(zipEntry -> {
                //get folder name for metadata files
                String[] metadataPath = zipEntry.getName().split("/");

                if (metadataPath.length >= 1){
                    Boolean multiStudy = metadataPath.length > 1;
                    String studyFolder = StringUtils.substringBeforeLast(zipEntry.getName(), "/");

                    //get metadata yaml file
                    ZipEntry sftpUploadYaml = zipFile.getEntry(zipEntry.getName());
                    if(sftpUploadYaml != null) {
                        //process study folder
                        log.info("study folder: " + studyFolder);
                        //hashmap mapping the folder and the files found in study folder
                        studyFilesUploaded.put(studyFolder, processStudyFolder(user.getId(), sftpSubmissionIds, zipFile,
                                sftpUploadYaml, studyFolder, multiStudy));

                    }
                    //if there is no metadata file,
                    else {
                        log.info("Required metadata to process study folder " + studyFolder + " not found");
                    }
                }
                else{
                    log.info("invalid metadata path "+ zipEntry.getName());
                    throw new BadDataException("Invalid SFTP ZipFile Structure. " + "Invalid metadata path "+ zipEntry.getName());
                }
            });
            //delete the downloaded zip file after processing is done.
            try {
                zipFile.close();
                FileUtils.delete(downloadedZipFile);
            } catch (IOException e) {
                throw new RuntimeException("Error occurred while attempting to delete the downloaded sftp zip file from resource folder: " + e.getMessage());
            }
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while attempting to read contents of zipfile for sftp processing: " + e.getMessage());
        } catch (ExecutionException e) {
            throw new RuntimeException("Error occurred while attempting to access S3 file for sftp processing: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException("Error occurred during sftp processing: " + e.getMessage());
        }
        return sftpSubmissionIds;
    }


    /**
     * processStudyFolder loads the metadata yaml containing the study and upload information
     * and upload the files within study folder
     *
     * @param inputStream - The contents of the metadata file
     *                    should follow path structure:  env/sftpUserId/zipFolderName.zip
     * @return yaml - map representation of metadata file
     */
    private Map<String, Object> processMetadataFile(InputStream inputStream) {
        Map<String, Object> yaml = new Yaml().load(inputStream);
        return yaml;
    }

    /**
     * processStudyFolder loads the metadata yaml containing the study and upload information
     * and upload the files within study folder
     *
     * @param sftpSubmissionIds - Hashset tracking the submissionIds of successful uploads
     * @param zipFile           - The zipfile object representing the zipfile uploaded via sftp
     * @param sftpUploadYaml    - The metadada yaml file found in zip
     * @param studyFolder       - The path of study folder.
     * @return List of files successfully uploaded in a given study folder
     */
    private List<String> processStudyFolder(Integer userId, HashSet<Integer> sftpSubmissionIds, ZipFile zipFile, ZipEntry sftpUploadYaml, String studyFolder,Boolean multiStudy) {
        List<String> filesUploaded = new ArrayList<>();
        try {
            //process yaml file
            Map<String, Object> yaml = processMetadataFile(zipFile.getInputStream(sftpUploadYaml));
            String studyUuid = (String) yaml.get("study_id");
            Study study = studyRepository.findStudyByUuidEqualsIgnoreCase(studyUuid.trim());
            //check if this study exist
            if (study != null) {
                Integer studyId = study.getId();
                log.info("study found " + study.getFileName());

                //check if there is already an open submission
                //get a list of non-validated "in progress" data submissions that are at most the upload step
                // that is related to study and UserId
                Optional<LkupSubmissionStep> lkupSubmissionStepdesc = lkupSubmissionStepRepository.findByDescription("Upload Files");
                Integer stepId = lkupSubmissionStepdesc.get().getId();

                LkupStatus inProgressStatus = lkupStatusRepository.findByUsageAndName(Constants.USAGE_DATA_SUBMISSION, Constants.STATUS_IN_PROGRESS)
                        .orElseThrow(()  -> new StatusNotFoundException(String.format("Invalid Data Submission Status: %s", Constants.STATUS_IN_PROGRESS)));
                Integer status = inProgressStatus.getId();

                List<DataSubmission> inProgressDataSubmissions = dataSubmissionRepository.findInProgressDataSubmission(studyId, userId, status, stepId);
                Integer submissionId = null;
                //if there are no 'in progress' submissions related to study, create a new submission
                if (inProgressDataSubmissions.isEmpty()) {
                    //create a new submission
                    submissionId = submissionService.createSubmission(study.getId(), userId);
                    log.info("new data submission for studyId: " + studyId + "with submissionId" + submissionId);
                } else {
                    //get the first available open data submission if there are already in-progress, non-validated submissions at upload step
                    submissionId = inProgressDataSubmissions.get(0).getId();
                    log.info("uploading files existing submissionId " + submissionId);
                }

                Integer finalSubmissionId = submissionId;

                //iterate through files in a study folder
                if(multiStudy){
                    //iterate through files in a study folder
                    zipFile.stream()
                            .filter(zipEntry -> zipEntry.getName().startsWith(studyFolder+"/")
                                    && !zipEntry.isDirectory()
                                    && !StringUtils.endsWithIgnoreCase(zipEntry.getName(), ".pub")
                                    && !StringUtils.endsWithIgnoreCase(zipEntry.getName(), ".yaml")
                                    && !StringUtils.containsIgnoreCase(zipEntry.getName(), ".DS_Store")) //file auto created on macOS
                            .forEach(zipEntry -> {
                                if (uploadFileSFTP(zipFile, zipEntry, studyUuid, finalSubmissionId, userId, true)) {
                                    filesUploaded.add(StringUtils.substringAfterLast(zipEntry.getName(), "/"));
                                    sftpSubmissionIds.add(finalSubmissionId);
                                }
                            });
                }
                else{
                    //iterate through files in a study folder
                    zipFile.stream()
                            .filter(zipEntry -> !zipEntry.isDirectory()
                                    && !StringUtils.endsWithIgnoreCase(zipEntry.getName(), ".pub")
                                    && !StringUtils.endsWithIgnoreCase(zipEntry.getName(), ".yaml")
                                    && !StringUtils.containsIgnoreCase(zipEntry.getName(), ".DS_Store")) //file auto created on macOS
                            .forEach(zipEntry -> {
                                if (uploadFileSFTP(zipFile, zipEntry, studyUuid, finalSubmissionId, userId, false)) {
                                    filesUploaded.add(StringUtils.substringAfterLast(zipEntry.getName(), "/"));
                                    sftpSubmissionIds.add(finalSubmissionId);
                                }
                            });
                }
                log.info("Files uploaded for study: " + studyId + ": " + filesUploaded.stream().toString());
            } else {
                log.info("Study with uuid: " + studyUuid + " not found ");
            }

        } catch (IOException e) {
            throw new RuntimeException("Error occurred when reading metadata yaml file for sftp processing: " + e.getMessage());
        }
        return filesUploaded;
    }

    /**
     * uploadFileSFTP uploads study files to the s3InitialUploadBucket and create a s3 table and datafile table entry for file
     * expected file path in upload bucket is 'studyUuid/submissionId/fileName'
     *
     * @param fileToUpload - the file to upload
     * @param studyUuid    - The study id for study that files are being uploaded
     * @param submissionId - is the data submission id related to this sftp upload
     * @return true if file uploaded to ingest bucket successfully
     */

    @Transactional
    public boolean uploadFileSFTP(ZipFile zipFile, ZipEntry fileToUpload, String studyUuid, Integer submissionId, Integer userId, Boolean multiStudy) {
        String fileName;
        if(multiStudy){
            fileName = StringUtils.substringAfterLast(fileToUpload.getName(), "/");
        }
        else{
            fileName = fileToUpload.getName();
        }        String key = studyUuid + "/" + submissionId + "/" + fileName;
        log.info("Uploading file to path: " + key);
        //upload file to initial upload bucket
        try {
            CompletedUpload sftpCompletedResponse = awsStorageService.uploadStream(zipFile.getInputStream(fileToUpload), transferManager, key);
            //Create instance of S3 file relating to file uploaded
            S3File s3File = new S3File();
            if (sftpCompletedResponse.response() == null) {
                s3File.setUploadSuccessful(false);
                return false;
            }
            PutObjectResponse completedResponse = sftpCompletedResponse.response();

            //configure s3 file
            s3File.setSubmissionId(submissionId);
            //after file upload is complete, make additional requests for file metadata
            CompletableFuture<GetObjectAttributesResponse> checksumResponse = client.getObjectAttributes(
                    GetObjectAttributesRequest.builder()
                            .bucket(s3InitialUploadBucket)
                            .key(key)
                            .objectAttributes(ObjectAttributes.CHECKSUM)
                            .build()
            );

            //set file metadata once additional requests complete
            String checksum = checksumResponse.get().checksum().checksumSHA256();
            s3File.setChecksumHash(checksum);
            s3File.setUploadSuccessful(true);
            s3File.setFileName(fileName);
            s3File.setFileSize(fileToUpload.getSize());
            s3File.setFileKey(key);
            s3File.setToBeRemoved(false);
            s3File.setUploadedAt(Timestamp.from(Instant.now()));
            s3File.setUploadedBy(userId);
            S3FileType type = typeRepository.findByName(getFileType(fileName));
            if(type == null) {
                type = typeRepository.findByName("other");
            }
            s3File.setFileTypeId(type.getId());
            s3File.setS3Etag(completedResponse.eTag().replace("\"", ""));
            s3File.setServerSideEncryption(completedResponse.serverSideEncryptionAsString());
            s3File.setFilePath(s3InitialUploadBucket + "/" + s3File.getFileKey());
            awsStorageService.getAndSetChecksumFromS3(s3File);

            //save file info to S3 table and datafile table
            fileRepository.saveAndFlush(s3File);
            dataFileService.createDataFile(s3File,userId);
            return true;

        } catch (Exception e) {
            log.warn("Error occurred when uploading file to s3 bucket via sftp: ", e);
            throw new RuntimeException("Error occurred when uploading file to s3 bucket via sftp: " + e.getMessage());
        }
    }

    public JSONObject getMessageJSON(String jsonString) {
        try {
            return new JSONObject(jsonString);
        } catch (JSONException e) {
            log.warn("Error parsing message from sqs: ", e.getMessage());
            throw new RuntimeException("Error parsing message from sqs: " + e.getMessage());
        }

    }

    /**
     * @param queueUrl      - The sqs queue that sent the message
     * @param receiptHandle - The receipt handle of the message to be deleted
     *                      deleteQueueMessage deletes a message from the queue once it has been processed.
     */
    private void deleteQueueMessage(String queueUrl, String receiptHandle) {
        DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build();
        DeleteMessageResponse deleteMessageResponse = DeleteMessageResponse.builder().build();
        sqsAsyncClient.deleteMessage(deleteMessageRequest).complete(deleteMessageResponse);
    }

    private String getFileType(String fileName){
        Tika tika = new Tika();
        String fileType = tika.detect(fileName);
        return fileType;
    }
}
