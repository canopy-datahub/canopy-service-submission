package ex.org.project.submissionService.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ex.org.project.submissionService.config.S3BucketConfig;
import ex.org.project.submissionService.models.S3File;
import ex.org.project.submissionService.models.S3FileType;
import ex.org.project.submissionService.models.Study;
import ex.org.project.submissionService.repositories.S3FileRepository;
import ex.org.project.submissionService.repositories.S3FileTypeRepository;
import ex.org.project.submissionService.repositories.StudyRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.BlockingInputStreamAsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@Slf4j
@Service
public class AwsStorageService implements StorageService {

    private final S3AsyncClient client;
    private final S3FileRepository fileRepository;
    private final S3FileTypeRepository typeRepository;
    private final StudyRepository studyRepository;
    private final S3TransferManager transferManager;
    private final String s3InitialUploadBucket;
    private final String s3ApprovedBucket;
    private final String uploadPortalBucket;


    @Autowired
    public AwsStorageService(S3AsyncClient client,
                             S3FileRepository fileRepository,
                             S3FileTypeRepository typeRepository,
                             S3TransferManager transferManager,
                             StudyRepository studyRepository,
                             S3BucketConfig s3BucketConfig
    ){
        this.client = client;
        this.fileRepository = fileRepository;
        this.typeRepository = typeRepository;
        this.studyRepository = studyRepository;
        this.transferManager = transferManager;
        this.s3InitialUploadBucket = s3BucketConfig.getInReview();
        this.s3ApprovedBucket = s3BucketConfig.getApproved();
        this.uploadPortalBucket = s3BucketConfig.getUploadPortal();
    }

    /**
     * Original method for file upload to S3. To be replaced by the other uploadFile method.
     * @param file data to be uploaded
     * @param submissionId submission that this file upload belongs to
     * @return the newly created S3File object containing metadata about the file
     */
    public S3File uploadFile(MultipartFile file, Integer submissionId, Integer userId){
        Study study = studyRepository.findByDataSubmission_Id(submissionId);
        S3File s3File = new S3File(file, submissionId, study.getUuid());
        s3File.setUploadedBy(userId);
        //get file type id from uploaded file type
        try {
            S3FileType type = typeRepository.findByName(s3File.getFileType());
            if(type == null) {
                type = typeRepository.findByName("other");
            }
            s3File.setFileTypeId(type.getId());
        } catch(DataAccessException e) {
            String errorMessage = "Error accessing database";
            log.error(errorMessage, e);
            s3File.setUploadSuccessful(false);
            s3File.setUploadErrorDescription(errorMessage);
            return s3File;
        }

        try {
            //async request to upload file to s3
            UploadRequest uploadRequest = UploadRequest.builder()
                    .putObjectRequest(
                            req -> req.bucket(s3InitialUploadBucket)
                                    .key(s3File.getFileKey())
                                    .checksumAlgorithm(ChecksumAlgorithm.SHA256)
                    )
                    .requestBody(AsyncRequestBody.fromInputStream(
                            file.getInputStream(),
                            file.getSize(),
                            new ScheduledThreadPoolExecutor(2)
                    )).build();
            Upload uploadResponse = transferManager.upload(uploadRequest);

            //once request is made, wait for response completion and set values
            PutObjectResponse completedResponse = uploadResponse.completionFuture().get().response();
            s3File.setS3Etag(completedResponse.eTag().replace("\"", ""));
            s3File.setServerSideEncryption(completedResponse.serverSideEncryptionAsString());
            s3File.setFilePath(s3InitialUploadBucket + "/" + s3File.getFileKey());

            //after file upload is complete, make additional requests for file metadata
            CompletableFuture<GetObjectAttributesResponse> checksumResponse = client.getObjectAttributes(
                    GetObjectAttributesRequest.builder()
                            .bucket(s3InitialUploadBucket)
                            .key(s3File.getFileKey())
                            .objectAttributes(ObjectAttributes.CHECKSUM)
                            .build()
            );

            //set file metadata once additional requests complete
            GetObjectAttributesResponse attrResponse = checksumResponse.get();
            if (attrResponse.checksum() != null) {
                String checksum = attrResponse.checksum().checksumSHA256();
                s3File.setChecksumHash(checksum);
            } else {
                log.warn("No checksum attribute returned from S3 for key: {}", s3File.getFileKey());
            }
            s3File.setUploadSuccessful(true);
            return s3File;

            //aws error handling
        } catch(CancellationException | ExecutionException | InterruptedException e){
            log.error("Error uploading file to AWS", e);
            s3File.setUploadSuccessful(false);
            s3File.setUploadErrorDescription("Error uploading file to AWS");
            return s3File;
        } catch(IOException e){
            log.error("Error reading data from file", e);
            s3File.setUploadSuccessful(false);
            s3File.setUploadErrorDescription("Error reading data from file");
            return s3File;
        }
    }

    /**
     * Updated file upload to S3 to be used in parallel stream
     * @param file data to be uploaded
     * @param submissionId submission that this file upload belongs to
     * @param studyUuid identifier of study this submission belongs to, used in s3 file path
     * @return the newly created (and transient) S3File object containing metadata about the file
     */
    public S3File uploadFile(MultipartFile file, Integer submissionId, String studyUuid, Integer userId){

        S3File s3File = new S3File(file, submissionId, studyUuid);
        s3File.setUploadedBy(userId);
        S3FileType type = typeRepository.findByName(s3File.getFileType());
        if(type == null) {
            type = typeRepository.findByName("other");
        }
        if (type != null) {
            s3File.setFileTypeId(type.getId());
        } else {
            log.warn("No S3FileType found for content type '{}' or fallback 'other'. fileTypeId will not be set.", s3File.getFileType());
        }

        Optional<PutObjectResponse> response = checkSuccess(uploadToS3(s3File, file, s3InitialUploadBucket));
        if(response.isEmpty()){
            s3File.setUploadSuccessful(false);
            return s3File;
        }

        PutObjectResponse completedResponse = response.get();
        s3File.setS3Etag(completedResponse.eTag().replace("\"", ""));
        s3File.setServerSideEncryption(completedResponse.serverSideEncryptionAsString());
        s3File.setFilePath(s3InitialUploadBucket + "/" + s3File.getFileKey());
        getAndSetChecksumFromS3(s3File);

        return s3File;
    }

    public S3File uploadPortalFile(MultipartFile file, Integer userId) {
        S3File s3File = new S3File(file);
        s3File.setUploadedBy(userId);
        S3FileType type = typeRepository.findByName(s3File.getFileType());
        if(type == null) {
            type = typeRepository.findByName("other");
        }
        s3File.setFileTypeId(type.getId());
        Optional<PutObjectResponse> response = checkSuccess(uploadToS3(s3File, file, uploadPortalBucket));
        if(response.isEmpty()){
            s3File.setUploadSuccessful(false);
            return s3File;
        }
        PutObjectResponse completedResponse = response.get();
        s3File.setS3Etag(completedResponse.eTag().replace("\"", ""));
        s3File.setServerSideEncryption(completedResponse.serverSideEncryptionAsString());
        s3File.setFilePath(uploadPortalBucket + "/" + s3File.getFileKey());
        getAndSetChecksumFromS3(s3File, uploadPortalBucket);
        return s3File;
    }

    /**
     * @param inputStream     - The file contents being uploaded
     * @param transferManager - To upload content from a stream of unknown size, use the S3TransferManager
     * @param key             - The name of the object.
     * @return - software.amazon.awssdk.transfer.s3.model.CompletedUpload - The result of the completed upload
     */
    public CompletedUpload uploadStream(InputStream inputStream, S3TransferManager transferManager, String key) {

        BlockingInputStreamAsyncRequestBody body =
                AsyncRequestBody.forBlockingInputStream(null); // 'null' indicates a stream will be provided later.
        Upload upload = transferManager.upload(builder -> builder
                .requestBody(body)
                .putObjectRequest(req -> req.bucket(s3InitialUploadBucket)
                        .key(key)
                        .checksumAlgorithm(ChecksumAlgorithm.SHA256))
                .build());

        // Provide the stream of data to be uploaded.
        body.writeInputStream(inputStream);
        return upload.completionFuture().join();
    }


    /**
     * Uploads a file to S3 based on metadata stored in an S3File object
     * @param s3File metadata object
     * @param file contains the data to be uploaded to S3
     * @return optional of the AWS async upload object if started successfully, empty optional otherwise
     */
    private Optional<Upload> uploadToS3(S3File s3File, MultipartFile file, String bucket){
        try {
            UploadRequest uploadRequest = UploadRequest.builder()
                    .putObjectRequest(
                            req -> req.bucket(bucket)
                                    .key(s3File.getFileKey())
                                    .checksumAlgorithm(ChecksumAlgorithm.SHA256)
                    )
                    .requestBody(AsyncRequestBody.fromBytes(file.getBytes()))
                    .build();
            return Optional.of(transferManager.upload(uploadRequest));
        } catch (IOException e){
            log.error("Error reading from MultipartFile.");
            s3File.setUploadSuccessful(false);
            s3File.setUploadErrorDescription("Error reading file contents");
            return Optional.empty();
        }
    }

    /**
     * Checks the success of a file upload
     * @param response contains the AWS async object to be joined and checked
     * @return optional of the response data if successful, empty object otherwise
     */
    private Optional<PutObjectResponse> checkSuccess(Optional<Upload> response){
        try {

            if(response.isPresent()){
                PutObjectResponse completedResponse = response.get().completionFuture().join().response();
                return Optional.of(completedResponse);
            } else {
                return Optional.empty();
            }
        } catch (CompletionException | CancellationException e){
            log.error("Error uploading file to S3");
            return Optional.empty();
        }
    }

    /**
     * Obtains the checksum of an object stored in S3 and stores it in the provided S3File object
     * @param s3File metadata object
     */
    public void getAndSetChecksumFromS3(S3File s3File){
        getAndSetChecksumFromS3(s3File, s3InitialUploadBucket);
    }


    private void getAndSetChecksumFromS3(S3File s3File, String bucket){
        CompletableFuture<GetObjectAttributesResponse> checksumResponse = client.getObjectAttributes(
                GetObjectAttributesRequest.builder()
                        .bucket(bucket)
                        .key(s3File.getFileKey())
                        .objectAttributes(ObjectAttributes.CHECKSUM)
                        .build()
        );

        try {
            GetObjectAttributesResponse attrResponse = checksumResponse.get();
            if (attrResponse.checksum() != null) {
                String checksum = attrResponse.checksum().checksumSHA256();
                s3File.setChecksumHash(checksum);
            } else {
                log.warn("No checksum attribute returned from S3 for key: {}", s3File.getFileKey());
            }
            s3File.setUploadSuccessful(true);
        } catch (CancellationException | ExecutionException | InterruptedException e){
            log.error("Error getting checksum value from s3 for key: {}", s3File.getFileKey(), e);
            s3File.setUploadSuccessful(true);
        }
    }

    public Boolean deleteFileFromS3(S3File s3File) {
        s3File.setS3FileKeyAndBucketFromPath();
        boolean wasDeleted = false;
        if(s3File.getFileKey() == null){
            log.error("S3 file deletion attempted with null file key");
            return wasDeleted;
        }
        try {
            CompletableFuture<DeleteObjectResponse> deletion = client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(s3File.getFileBucket())
                            .key(s3File.getFileKey())
                            .build()
            );
            deletion.get();
            wasDeleted = true;
        } catch(CancellationException | ExecutionException | InterruptedException e){
            log.error("Error deleting file from s3 storage: " + s3File.getFilePath(), e);
            s3File.setUploadErrorDescription("Error deleting file from s3 storage");
        }
        return wasDeleted;
    }

    public boolean deleteStudyFromS3(Study study) {
        // Extract bucket name and object key from fileUrl
       study.extractBucketAndObjectKeyFromS3Uri(study.getFileUrl());
        boolean wasDeleted = false;

        // Delete the file from the S3 bucket using the bucket name and object key
        try {
            CompletableFuture<DeleteObjectResponse> deletion = client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(study.getBucketName())
                            .key(study.getObjectKey())
                            .build()
            );
            deletion.get();
            wasDeleted = true;

        } catch (CancellationException | ExecutionException | InterruptedException e) {
            log.error("Error deleting file from s3 storage: " + study.getFileUrl(), e);
        }
        return wasDeleted;
    }

    public InputStream getS3FileContent(S3File s3File){
        s3File.setS3FileKeyAndBucketFromPath();
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3File.getFileBucket())
                    .key(s3File.getFileKey())
                    .build();
            return client.getObject(getObjectRequest, AsyncResponseTransformer.toBlockingInputStream()).get();
        } catch (CancellationException | InterruptedException | ExecutionException e){
            throw new RuntimeException("Error occurred while attempting to access S3 file: "+ e.getMessage());
        }
    }

    public List<JsonNode> getGZipS3FileContent(String filepath) {
        String[] fileDetails = filepath.split("/", 2);
        List<JsonNode> jsonNodes = new ArrayList<>();
        try {
            //get all GZIP files produced from the job and uploaded to S3
            ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder()
                    .bucket(fileDetails[0])
                    .prefix(StringUtils.substringBeforeLast(fileDetails[1],"/"))
                    .build();
            List<S3Object> listGZIPs = client.listObjects(listObjectsRequest).get().contents();
            //for each gzip file, extract it's content and add the PII results to jsonNodes
            listGZIPs.stream().forEach(s3Object -> {
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(fileDetails[0])
                        .key(s3Object.key())
                        .build();
                InputStream inputStream = null;
                try {
                    inputStream = client.getObject(getObjectRequest, AsyncResponseTransformer.toBlockingInputStream()).get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException("Error occurred while attempting to access GZIP S3 file: " + e.getMessage());
                }
                final GZIPInputStream zipInputStream;
                try {
                    //Convert it to GZIP stream
                    zipInputStream = new GZIPInputStream(inputStream);
                    BufferedReader in = new BufferedReader(new InputStreamReader(zipInputStream));
                    ObjectMapper objectMapper = new ObjectMapper();
                    String contentStr;
                    while ((contentStr = in.readLine()) != null) {
                        jsonNodes.add(objectMapper.readTree(contentStr));
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error occurred while attempting to access read gzip file with macie scan results: " + e.getMessage());
                }
            });
        } catch (CancellationException | InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error occurred while attempting to access S3 file: " + e.getMessage());
        }
        return jsonNodes;
    }

    /**
     * Moves a list of S3Files to the approved bucket in S3
     * @param s3Files List of S3Files to be moved
     * @return true if successful, false otherwise
     */
    public boolean moveToApproved(List<S3File> s3Files){
        Map<Integer, Copy> s3Responses = s3Files.stream()
                .collect(Collectors.toMap(
                        S3File::getId,
                        s3File ->  moveToBucket(s3File, s3ApprovedBucket)
                ));
        Map<Integer, Optional<CopyObjectResult>> results = s3Responses.entrySet().stream()
                .collect(Collectors.toMap(
                        s3Response -> s3Response.getKey(),
                        s3Response -> checkSuccess(s3Response.getValue())
                ));
        results.values().removeIf(Optional::isEmpty);

        if(results.isEmpty()){
            log.error("Files could not be copied to new bucket. Attempted to copy S3Files: "
                    + s3Files.stream().map(S3File::getId).toList()
            );
            return false;
        }

        if(results.size() < s3Files.size()){
            log.error("Error copying files to approved bucket");
            log.error("Attempted to copy S3Files: " + s3Files.stream().map(S3File::getId).toList() + "\nSuccessfully moved: " + results.keySet());
        }
        for(S3File s3File : s3Files){
            if(results.containsKey(s3File.getId())){
                CopyObjectResult result = results.get(s3File.getId()).get();
                applyResponseAndCompleteMove(s3File, result, s3ApprovedBucket);
            }
        }
        return true;
    }

    /**
     * Method to join Copy completion futures
     * @param response Copy response object from AWS S3 client
     * @return Optional of result if successful, empty Optional otherwise
     */
    private Optional<CopyObjectResult> checkSuccess(Copy response){
        try {
            CopyObjectResponse completedResponse = response.completionFuture().join().response();
            return Optional.of(completedResponse.copyObjectResult());
        } catch (CompletionException e){
            log.error("Error copying file between S3 buckets");
            return Optional.empty();
        }
    }

    /**
     * Method to check if a file was moved correctly, clean up the s3 filesystem,
     * apply the new file path, and save the updated S3File entity in the database
     * @param s3File file that has been moved
     * @param result the response of the move action from AWS S3
     * @param toBucket S3 bucket the file was moved to
     * @return true if successful, false otherwise
     */
    private boolean applyResponseAndCompleteMove(S3File s3File, CopyObjectResult result, String toBucket){
        if((s3File.getChecksumHash() != null) && (!s3File.getChecksumHash().equals(result.checksumSHA256()))){
            log.error("Data corruption may have occurred. Please check S3File ID: "  + s3File.getId());
            return false;
        }
        deleteFileFromS3(s3File);
        s3File.setFilePath(toBucket + "/" + s3File.getNewFileKey());
        s3File.setFileName(FilenameUtils.getName(s3File.getNewFileKey()));
        fileRepository.save(s3File);
        return true;
    }

    /**
     * Method to move a file from bucket to bucket in S3
     * @param s3File file to be moved
     * @param toBucket bucket the file is being moved into
     * @return Copy response object from AWS S3 client
     */
    private Copy moveToBucket(S3File s3File, String toBucket){
        if(s3File.getNewFileKey() == null) {
            log.error(String.format("New file key not set for ID %d", s3File.getId()));
        }
        s3File.setS3FileKeyAndBucketFromPath();
        CopyRequest copyRequest = CopyRequest.builder()
                .copyObjectRequest(
                        req -> req.sourceBucket(s3File.getFileBucket())
                        .sourceKey(s3File.getFileKey())
                        .destinationBucket(toBucket)
                        .destinationKey(s3File.getNewFileKey()))
                .build();
        return transferManager.copy(copyRequest);
    }

    /**
     * Method delete sftp package from S3 after upload is complete
      @param zipFile       - The string representing path to the zipfile uploaded via sftp
      *                         should follow path structure:  env/sftpUserId/zipFolderName.zip
      * @param bucket       - sftp upload bucket
      * @param region       -    sftp bucket region
     */
    public void deleteSFTPPackageFromS3(String zipFile, String bucket, String region) {

        try (S3AsyncClient sftpClient = S3AsyncClient.builder()
                .region(Region.of(region))
                .build()) {

            CompletableFuture<DeleteObjectResponse> deletion = sftpClient.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(zipFile)
                            .build()
            );
            deletion.get();
            log.info("successfully deleted zip file {} from s3 storage: ",zipFile);

        } catch (CancellationException | ExecutionException | InterruptedException e) {
            log.error("Error deleting zipfile from s3 storage: " + zipFile, e);
        }
    }
}
