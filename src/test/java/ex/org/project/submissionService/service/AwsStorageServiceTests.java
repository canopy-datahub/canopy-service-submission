package ex.org.project.submissionService.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ex.org.project.submissionService.config.S3BucketConfig;
import ex.org.project.submissionService.models.S3File;
import ex.org.project.submissionService.models.S3FileType;
import ex.org.project.submissionService.models.Study;
import ex.org.project.submissionService.repositories.S3FileRepository;
import ex.org.project.submissionService.repositories.S3FileTypeRepository;
import ex.org.project.submissionService.repositories.StudyRepository;
import ex.org.project.submissionService.services.AwsStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class AwsStorageServiceTests {

    @Mock
    private Appender mockedAppender;

    @Captor
    private ArgumentCaptor<LoggingEvent> loggingEventCaptor;

    private final S3AsyncClient client = mock(S3AsyncClient.class);
    private final S3FileRepository fileRepository = mock(S3FileRepository.class);
    private final S3FileTypeRepository typeRepository = mock(S3FileTypeRepository.class);
    private final S3TransferManager transferManager = mock(S3TransferManager.class);
    private final StudyRepository studyRepository = mock(StudyRepository.class);
    private final String s3Bucket = "testBucket";
    private final String newS3Bucket = "newBucket";

    @Captor
    ArgumentCaptor<UploadRequest> uploadRequestCaptor;

    private final AwsStorageService storageService = new AwsStorageService(
            client, fileRepository, typeRepository, transferManager,
            studyRepository, getS3BucketConfig()
    );

    S3BucketConfig getS3BucketConfig() {
        S3BucketConfig bucketConfig = new S3BucketConfig();
        bucketConfig.setInReview("testBucket");
        bucketConfig.setApproved("newBucket");
        return bucketConfig;
    }

    @Test
    void testUploadFile_HappyPath() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hello.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World!".getBytes()
        );
        Study mockStudy = new Study();
        mockStudy.setUuid("uuid-12345");
        Integer submissionId = 25;
        S3FileType mockFileType = new S3FileType();
        mockFileType.setId(4);
        CompletedUpload mockCompletedUpload = CompletedUpload.builder()
                .response(PutObjectResponse.builder()
                        .eTag("testEtag")
                        .serverSideEncryption("testEncryption")
                        .build())
                .build();
        CompletableFuture<CompletedUpload> mockUploadFuture = mock(CompletableFuture.class);
        Upload mockUploadResponse = mock(Upload.class);
        S3Utilities mockUtilities =  mock(S3Utilities.class);
        CompletableFuture<GetObjectAttributesResponse> mockAttributeResponse = mock(CompletableFuture.class);
        GetObjectAttributesResponse mockChecksumResponse = GetObjectAttributesResponse.builder()
                .checksum(Checksum.builder().checksumSHA256("testSHA256").build()).build();

        when(studyRepository.findByDataSubmission_Id(25))
                .thenReturn(mockStudy);
        when(typeRepository.findByName(anyString()))
                .thenReturn(mockFileType);
        when(transferManager.upload(any(UploadRequest.class)))
                .thenReturn(mockUploadResponse);
        when(mockUploadResponse.completionFuture())
                .thenReturn(mockUploadFuture);
        when(mockUploadFuture.get()).thenReturn(mockCompletedUpload);
        when(client.getObjectAttributes(any(GetObjectAttributesRequest.class)))
                .thenReturn(mockAttributeResponse);
        when(client.utilities())
                .thenReturn(mockUtilities);
        when(mockAttributeResponse.get())
                .thenReturn(mockChecksumResponse);
        S3File idFile = new S3File();
        idFile.setId(1);
        when(fileRepository.saveAndFlush(any(S3File.class)))
                .thenReturn(idFile);

        S3File returnFile = storageService.uploadFile(file, submissionId, 1);
        assertEquals("hello.txt", returnFile.getFileName());
        assertEquals("testEtag", returnFile.getS3Etag());
        assertEquals("testEncryption", returnFile.getServerSideEncryption());
        assertEquals("testSHA256", returnFile.getChecksumHash());
        assertEquals(25, returnFile.getSubmissionId());
        assertNull(returnFile.getId());
        assertEquals("uuid-12345/25/hello.txt", returnFile.getFileKey());
        assertEquals(4, returnFile.getFileTypeId());
        assertTrue(returnFile.getUploadSuccessful());
    }

    @Test
    void testUploadFile_HappyPathWithUnknownFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hello.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World!".getBytes()
        );
        Study mockStudy = new Study();
        mockStudy.setUuid("uuid-12345");
        Integer submissionId = 25;
        S3FileType mockFileType = new S3FileType();
        mockFileType.setId(9);
        CompletedUpload mockCompletedUpload = CompletedUpload.builder()
                .response(PutObjectResponse.builder()
                        .eTag("testEtag")
                        .serverSideEncryption("testEncryption")
                        .build())
                .build();
        CompletableFuture<CompletedUpload> mockUploadFuture = mock(CompletableFuture.class);
        Upload mockUploadResponse = mock(Upload.class);
        S3Utilities mockUtilities =  mock(S3Utilities.class);
        CompletableFuture<GetObjectAttributesResponse> mockAttributeResponse = mock(CompletableFuture.class);
        GetObjectAttributesResponse mockChecksumResponse = GetObjectAttributesResponse.builder()
                .checksum(Checksum.builder().checksumSHA256("testSHA256").build()).build();

        when(studyRepository.findByDataSubmission_Id(25))
                .thenReturn(mockStudy);
        when(typeRepository.findByName(MediaType.TEXT_PLAIN_VALUE))
                .thenReturn(null);
        when(typeRepository.findByName("other"))
                .thenReturn(mockFileType);
        when(transferManager.upload(any(UploadRequest.class)))
                .thenReturn(mockUploadResponse);
        when(mockUploadResponse.completionFuture())
                .thenReturn(mockUploadFuture);
        when(mockUploadFuture.get())
                .thenReturn(mockCompletedUpload);
        when(client.getObjectAttributes(any(GetObjectAttributesRequest.class)))
                .thenReturn(mockAttributeResponse);
        when(client.utilities())
                .thenReturn(mockUtilities);
        when(mockAttributeResponse.get())
                .thenReturn(mockChecksumResponse);
        S3File idFile = new S3File();
        idFile.setId(1);
        when(fileRepository.saveAndFlush(any(S3File.class)))
                .thenReturn(idFile);

        S3File returnFile = storageService.uploadFile(file, submissionId, 1);
        assertEquals("hello.txt", returnFile.getFileName());
        assertEquals("testEtag", returnFile.getS3Etag());
        assertEquals("testEncryption", returnFile.getServerSideEncryption());
        assertEquals("testSHA256", returnFile.getChecksumHash());
        assertEquals(25, returnFile.getSubmissionId());
        assertNull(returnFile.getId());
        assertEquals("uuid-12345/25/hello.txt", returnFile.getFileKey());
        assertEquals(9, returnFile.getFileTypeId());
        assertTrue(returnFile.getUploadSuccessful());
    }

    @Test
    void testUploadFile_IOExceptionWhenReadingFileStream() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        Integer submissionId = 25;
        S3FileType mockFileType = new S3FileType();
        mockFileType.setId(4);
        Study mockStudy = new Study();
        mockStudy.setUuid("uuid-12345");

        when(studyRepository.findByDataSubmission_Id(25))
                .thenReturn(mockStudy);
        when(typeRepository.findByName(anyString()))
                .thenReturn(mockFileType);
        when(file.getInputStream())
                .thenThrow(IOException.class);

        S3File returnFile = storageService.uploadFile(file, submissionId, 1);
        assertNull(returnFile.getS3Etag());
        assertNull(returnFile.getServerSideEncryption());
        assertNull(returnFile.getChecksumHash());
        assertEquals(25, returnFile.getSubmissionId());
        assertNull(returnFile.getId());
        assertEquals(4, returnFile.getFileTypeId());
        assertFalse(returnFile.getUploadSuccessful());
    }

    @Test
    void testUploadFile_AwsCancellationException() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hello.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World!".getBytes()
        );
        Study mockStudy = new Study();
        mockStudy.setUuid("uuid-12345");
        Integer submissionId = 25;
        S3FileType mockFileType = new S3FileType();
        mockFileType.setId(4);
        CompletableFuture<CompletedUpload> mockUploadFuture = mock(CompletableFuture.class);
        Upload mockUploadResponse = mock(Upload.class);

        when(studyRepository.findByDataSubmission_Id(25))
                .thenReturn(mockStudy);
        when(typeRepository.findByName(anyString()))
                .thenReturn(mockFileType);
        when(transferManager.upload(any(UploadRequest.class)))
                .thenReturn(mockUploadResponse);
        when(mockUploadResponse.completionFuture())
                .thenReturn(mockUploadFuture);
        when(mockUploadFuture.get())
                .thenThrow(CancellationException.class);

        S3File returnFile = storageService.uploadFile(file, submissionId, 1);
        assertNull(returnFile.getS3Etag());
        assertNull(returnFile.getServerSideEncryption());
        assertNull(returnFile.getChecksumHash());
        assertEquals(25, returnFile.getSubmissionId());
        assertNull(returnFile.getId());
        assertEquals(4, returnFile.getFileTypeId());
        assertFalse(returnFile.getUploadSuccessful());
    }

    @Test
    void testUploadFile_AwsExecutionException() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hello.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World!".getBytes()
        );
        Study mockStudy = new Study();
        mockStudy.setUuid("uuid-12345");
        Integer submissionId = 25;
        S3FileType mockFileType = new S3FileType();
        mockFileType.setId(4);
        CompletableFuture<CompletedUpload> mockUploadFuture = mock(CompletableFuture.class);
        Upload mockUploadResponse = mock(Upload.class);

        when(studyRepository.findByDataSubmission_Id(25))
                .thenReturn(mockStudy);
        when(typeRepository.findByName(anyString()))
                .thenReturn(mockFileType);
        when(transferManager.upload(any(UploadRequest.class)))
                .thenReturn(mockUploadResponse);
        when(mockUploadResponse.completionFuture())
                .thenReturn(mockUploadFuture);
        when(mockUploadFuture.get())
                .thenThrow(ExecutionException.class);

        S3File returnFile = storageService.uploadFile(file, submissionId, 1);
        assertNull(returnFile.getS3Etag());
        assertNull(returnFile.getServerSideEncryption());
        assertNull(returnFile.getChecksumHash());
        assertEquals(25, returnFile.getSubmissionId());
        assertNull(returnFile.getId());
        assertEquals(4, returnFile.getFileTypeId());
        assertFalse(returnFile.getUploadSuccessful());
    }

    @Test
    void testUploadFile_AwsInterruptedException() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hello.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World!".getBytes()
        );
        Study mockStudy = new Study();
        mockStudy.setUuid("uuid-12345");
        Integer submissionId = 25;
        S3FileType mockFileType = new S3FileType();
        mockFileType.setId(4);
        CompletableFuture<CompletedUpload> mockUploadFuture = mock(CompletableFuture.class);
        Upload mockUploadResponse = mock(Upload.class);

        when(studyRepository.findByDataSubmission_Id(25))
                .thenReturn(mockStudy);
        when(typeRepository.findByName(anyString()))
                .thenReturn(mockFileType);
        when(transferManager.upload(any(UploadRequest.class)))
                .thenReturn(mockUploadResponse);
        when(mockUploadResponse.completionFuture())
                .thenReturn(mockUploadFuture);
        when(mockUploadFuture.get())
                .thenThrow(InterruptedException.class);

        S3File returnFile = storageService.uploadFile(file, submissionId, 1);
        assertNull(returnFile.getS3Etag());
        assertNull(returnFile.getServerSideEncryption());
        assertNull(returnFile.getChecksumHash());
        assertEquals(25, returnFile.getSubmissionId());
        assertNull(returnFile.getId());
        assertEquals(4, returnFile.getFileTypeId());
        assertFalse(returnFile.getUploadSuccessful());
    }

    @Test
    void testUploadFile_DatabaseDataAccessException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hello.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World!".getBytes()
        );
        Study mockStudy = new Study();
        mockStudy.setUuid("uuid-12345");
        Integer submissionId = 25;
        S3FileType mockFileType = new S3FileType();
        mockFileType.setId(4);

        when(studyRepository.findByDataSubmission_Id(25))
                .thenReturn(mockStudy);
        when(typeRepository.findByName(anyString()))
                .thenThrow(DataRetrievalFailureException.class);

        S3File returnFile = storageService.uploadFile(file, submissionId, 1);
        assertNull(returnFile.getS3Etag());
        assertNull(returnFile.getServerSideEncryption());
        assertNull(returnFile.getChecksumHash());
        assertEquals(25, returnFile.getSubmissionId());
        assertNull(returnFile.getId());
        assertNull(returnFile.getFileTypeId());
        assertFalse(returnFile.getUploadSuccessful());
    }

    private MockMultipartFile getMockMultipartFile(){
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hello.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World!".getBytes()
        );
        return file;
    }

    @Test
    void testNewUploadFile_HappyPath() throws Exception{
        MockMultipartFile file = getMockMultipartFile();
        String uuid = "uuid-12345";
        Integer submissionId = 25;
        S3FileType mockFileType = new S3FileType();
        mockFileType.setId(4);

        Upload mockUpload = mock(Upload.class);
        CompletableFuture<CompletedUpload> mockUploadFuture = mock(CompletableFuture.class);
        CompletedUpload mockCompletedUpload = CompletedUpload.builder()
                .response(PutObjectResponse.builder()
                        .eTag("testEtag")
                        .serverSideEncryption("testEncryption")
                        .build())
                .build();
        CompletableFuture<GetObjectAttributesResponse> mockAttributeResponse = mock(CompletableFuture.class);
        GetObjectAttributesResponse mockChecksumResponse = GetObjectAttributesResponse.builder()
                .checksum(Checksum.builder().checksumSHA256("testSHA256").build()).build();

        when(typeRepository.findByName(anyString()))
                .thenReturn(mockFileType);
        when(transferManager.upload(any(UploadRequest.class)))
                .thenReturn(mockUpload);
        when(mockUpload.completionFuture())
                .thenReturn(mockUploadFuture);
        when(mockUploadFuture.join()).thenReturn(mockCompletedUpload);
        when(client.getObjectAttributes(any(GetObjectAttributesRequest.class)))
                .thenReturn(mockAttributeResponse);
        when(mockAttributeResponse.get())
                .thenReturn(mockChecksumResponse);

        S3File returnFile = storageService.uploadFile(file, submissionId, uuid, 1);

        verify(transferManager).upload(uploadRequestCaptor.capture());
        UploadRequest captorValue = uploadRequestCaptor.getValue();

        assertEquals("testBucket", captorValue.putObjectRequest().bucket());
        assertEquals("uuid-12345/25/hello.txt", captorValue.putObjectRequest().key());
        assertEquals("hello.txt", returnFile.getFileName());
        assertEquals("testEtag", returnFile.getS3Etag());
        assertEquals("testEncryption", returnFile.getServerSideEncryption());
        assertEquals("testSHA256", returnFile.getChecksumHash());
        assertEquals(25, returnFile.getSubmissionId());
        assertEquals("uuid-12345/25/hello.txt", returnFile.getFileKey());
        assertEquals(4, returnFile.getFileTypeId());
        assertTrue(returnFile.getUploadSuccessful());
    }

    @Test
    void testNewUploadFile_HappyPathUnknownFileType() throws Exception{
        MockMultipartFile file = getMockMultipartFile();
        String uuid = "uuid-12345";
        Integer submissionId = 25;
        S3FileType mockFileType = new S3FileType();
        mockFileType.setId(9);

        Upload mockUpload = mock(Upload.class);
        CompletableFuture<CompletedUpload> mockUploadFuture = mock(CompletableFuture.class);
        CompletedUpload mockCompletedUpload = CompletedUpload.builder()
                .response(PutObjectResponse.builder()
                        .eTag("testEtag")
                        .serverSideEncryption("testEncryption")
                        .build())
                .build();
        CompletableFuture<GetObjectAttributesResponse> mockAttributeResponse = mock(CompletableFuture.class);
        GetObjectAttributesResponse mockChecksumResponse = GetObjectAttributesResponse.builder()
                .checksum(Checksum.builder().checksumSHA256("testSHA256").build()).build();

        when(typeRepository.findByName(MediaType.TEXT_PLAIN_VALUE))
                .thenReturn(null);
        when(typeRepository.findByName("other"))
                .thenReturn(mockFileType);
        when(transferManager.upload(any(UploadRequest.class)))
                .thenReturn(mockUpload);
        when(mockUpload.completionFuture())
                .thenReturn(mockUploadFuture);
        when(mockUploadFuture.join()).thenReturn(mockCompletedUpload);
        when(client.getObjectAttributes(any(GetObjectAttributesRequest.class)))
                .thenReturn(mockAttributeResponse);
        when(mockAttributeResponse.get())
                .thenReturn(mockChecksumResponse);

        S3File returnFile = storageService.uploadFile(file, submissionId, uuid, 1);

        verify(transferManager).upload(uploadRequestCaptor.capture());
        UploadRequest captorValue = uploadRequestCaptor.getValue();

        assertEquals("testBucket", captorValue.putObjectRequest().bucket());
        assertEquals("uuid-12345/25/hello.txt", captorValue.putObjectRequest().key());
        assertEquals("hello.txt", returnFile.getFileName());
        assertEquals("testEtag", returnFile.getS3Etag());
        assertEquals("testEncryption", returnFile.getServerSideEncryption());
        assertEquals("testSHA256", returnFile.getChecksumHash());
        assertEquals(25, returnFile.getSubmissionId());
        assertEquals("uuid-12345/25/hello.txt", returnFile.getFileKey());
        assertEquals(9, returnFile.getFileTypeId());
        assertTrue(returnFile.getUploadSuccessful());
    }

    @Test
    void testNewUploadFile_IOExceptionWhenReadingFile() throws Exception{
        MultipartFile file = mock(MultipartFile.class);
        String uuid = "uuid-12345";
        Integer submissionId = 25;
        S3FileType mockFileType = new S3FileType();
        mockFileType.setId(4);

        when(typeRepository.findByName(anyString()))
                .thenReturn(mockFileType);
        when(file.getBytes())
                .thenThrow(IOException.class);

        S3File returnFile = storageService.uploadFile(file, submissionId, uuid, 1);

        assertNull(returnFile.getS3Etag());
        assertNull(returnFile.getServerSideEncryption());
        assertNull(returnFile.getChecksumHash());
        assertEquals(25, returnFile.getSubmissionId());
        assertNull(returnFile.getId());
        assertEquals(4, returnFile.getFileTypeId());
        assertFalse(returnFile.getUploadSuccessful());
    }

    @Test
    void testNewUploadFile_AwsException(){
        MockMultipartFile file = getMockMultipartFile();
        String uuid = "uuid-12345";
        Integer submissionId = 25;
        S3FileType mockFileType = new S3FileType();
        mockFileType.setId(4);

        Upload mockUpload = mock(Upload.class);
        CompletableFuture<CompletedUpload> mockUploadFuture = mock(CompletableFuture.class);
        CompletedUpload mockCompletedUpload = CompletedUpload.builder()
                .response(PutObjectResponse.builder()
                        .eTag("testEtag")
                        .serverSideEncryption("testEncryption")
                        .build())
                .build();
        CompletableFuture<GetObjectAttributesResponse> mockAttributeResponse = mock(CompletableFuture.class);
        GetObjectAttributesResponse mockChecksumResponse = GetObjectAttributesResponse.builder()
                .checksum(Checksum.builder().checksumSHA256("testSHA256").build()).build();

        when(typeRepository.findByName(anyString()))
                .thenReturn(mockFileType);
        when(transferManager.upload(any(UploadRequest.class)))
                .thenReturn(mockUpload);
        when(mockUpload.completionFuture())
                .thenReturn(mockUploadFuture);
        when(mockUploadFuture.join())
                .thenThrow(CompletionException.class);

        S3File returnFile = storageService.uploadFile(file, submissionId, uuid, 1);

        verify(transferManager).upload(uploadRequestCaptor.capture());
        UploadRequest captorValue = uploadRequestCaptor.getValue();

        assertEquals("testBucket", captorValue.putObjectRequest().bucket());
        assertEquals("uuid-12345/25/hello.txt", captorValue.putObjectRequest().key());
        assertEquals("hello.txt", returnFile.getFileName());
        assertNull(returnFile.getS3Etag());
        assertNull(returnFile.getServerSideEncryption());
        assertNull(returnFile.getChecksumHash());
        assertEquals(25, returnFile.getSubmissionId());
        assertNull(returnFile.getId());
        assertEquals(4, returnFile.getFileTypeId());
        assertFalse(returnFile.getUploadSuccessful());
    }

    private List<S3File> getMockS3Files(){
        S3File file1 = new S3File();
        file1.setId(1);
        file1.setFileName("file1.txt");
        file1.setFilePath("bucket/study/submission/file1.txt");
        file1.setNewFileKey("study/file1_v1.txt");
        file1.setChecksumHash("1");

        S3File file2 = new S3File();
        file2.setId(2);
        file2.setFileName("file2.txt");
        file2.setFilePath("bucket/study/submission/file2.txt");
        file2.setNewFileKey("study/file2_v1.txt");
        file2.setChecksumHash("2");

        S3File file3 = new S3File();
        file3.setId(3);
        file3.setFileName("file3.txt");
        file3.setFilePath("bucket/study/submission/file3.txt");
        file3.setNewFileKey("study/file3_v1.txt");
        file3.setChecksumHash("3");

        List<S3File> list = new ArrayList<>(3);
        list.add(file1);
        list.add(file2);
        list.add(file3);
        return list;
    }

    private List<CopyObjectResponse> getMockCopyObjectResponses(){
        CopyObjectResponse response1 = CopyObjectResponse.builder()
                .copyObjectResult(CopyObjectResult.builder().checksumSHA256("1").build())
                .build();
        CopyObjectResponse response2 = CopyObjectResponse.builder()
                .copyObjectResult(CopyObjectResult.builder().checksumSHA256("2").build())
                .build();
        CopyObjectResponse response3 = CopyObjectResponse.builder()
                .copyObjectResult(CopyObjectResult.builder().checksumSHA256("3").build())
                .build();
        List<CopyObjectResponse> list = new ArrayList<>(3);
        list.add(response1);
        list.add(response2);
        list.add(response3);
        return list;
    }

    @Test
    void testMoveToApproved_HappyPath() throws Exception{
        List<S3File> files = getMockS3Files();
        Copy copy = mock(Copy.class);
        CompletableFuture<CompletedCopy> copyFuture = mock(CompletableFuture.class);
        CompletedCopy completedCopy = mock(CompletedCopy.class);
        List<CopyObjectResponse> mockResponses = getMockCopyObjectResponses();

        CompletableFuture<DeleteObjectResponse> deletionFuture = mock(CompletableFuture.class);
        DeleteObjectResponse deleteResponse = DeleteObjectResponse.builder().deleteMarker(true).build();

        when(transferManager.copy(any(CopyRequest.class)))
                .thenReturn(copy);
        when(copy.completionFuture())
                .thenReturn(copyFuture);
        when(copyFuture.join())
                .thenReturn(completedCopy);
        when(completedCopy.response())
                .thenReturn(mockResponses.get(0), mockResponses.get(1), mockResponses.get(2));
        when(client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(deletionFuture);
        when(deletionFuture.get())
                .thenReturn(deleteResponse);

        boolean response = storageService.moveToApproved(files);

        assertTrue(response);
        assertEquals("newBucket/study/file1_v1.txt", files.get(0).getFilePath());
        assertEquals("file1_v1.txt", files.get(0).getFileName());
        assertEquals("1", files.get(0).getChecksumHash());
        assertEquals("newBucket/study/file2_v1.txt", files.get(1).getFilePath());
        assertEquals("file2_v1.txt", files.get(1).getFileName());
        assertEquals("2", files.get(1).getChecksumHash());
        assertEquals("newBucket/study/file3_v1.txt", files.get(2).getFilePath());
        assertEquals("file3_v1.txt", files.get(2).getFileName());
        assertEquals("3", files.get(2).getChecksumHash());
    }

    @Test
    void testMoveToApproved_OneFileMoveError() throws Exception{
        List<S3File> files = getMockS3Files();
        Copy copy = mock(Copy.class);
        CompletableFuture<CompletedCopy> copyFuture = mock(CompletableFuture.class);
        CompletedCopy completedCopy = mock(CompletedCopy.class);
        List<CopyObjectResponse> mockResponses = getMockCopyObjectResponses();

        CompletableFuture<DeleteObjectResponse> deletionFuture = mock(CompletableFuture.class);
        DeleteObjectResponse deleteResponse = DeleteObjectResponse.builder().deleteMarker(true).build();

        when(transferManager.copy(any(CopyRequest.class)))
                .thenReturn(copy);
        when(copy.completionFuture())
                .thenReturn(copyFuture);
        when(copyFuture.join())
                .thenThrow(new CompletionException("test", new Throwable()))
                .thenReturn(completedCopy);
        when(completedCopy.response())
                .thenReturn(mockResponses.get(1), mockResponses.get(2));
        when(client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(deletionFuture);
        when(deletionFuture.get())
                .thenReturn(deleteResponse);

        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(mockedAppender);
        root.setLevel(Level.INFO);

        boolean response = storageService.moveToApproved(files);

        verify(mockedAppender, times(3)).doAppend(loggingEventCaptor.capture());

        assertTrue(response);
        files.forEach(file -> {
            assertTrue(file.getChecksumHash() != null);
        });
        assertEquals("bucket/study/submission/" + files.get(0).getFileName(), files.get(0).getFilePath());
        assertEquals("newBucket/study/file2_v1.txt", files.get(1).getFilePath());
        assertEquals("newBucket/study/file3_v1.txt", files.get(2).getFilePath());
        loggingEventCaptor.getAllValues().stream().forEach(e -> {
            assertEquals(Level.ERROR, e.getLevel());
        });
    }

    @Test
    void testMoveToApproved_AllFileMoveErrors() throws Exception{
        List<S3File> files = getMockS3Files();
        Copy copy = mock(Copy.class);
        CompletableFuture<CompletedCopy> copyFuture = mock(CompletableFuture.class);

        when(transferManager.copy(any(CopyRequest.class)))
                .thenReturn(copy);
        when(copy.completionFuture())
                .thenReturn(copyFuture);
        when(copyFuture.join())
                .thenThrow(new CompletionException("test", new Throwable()));

        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(mockedAppender);
        root.setLevel(Level.INFO);

        boolean response = storageService.moveToApproved(files);

        verify(mockedAppender, times(4)).doAppend(loggingEventCaptor.capture());

        assertFalse(response);
        files.forEach(file -> {
            assertTrue(file.getChecksumHash() != null);
        });
        assertEquals("bucket/study/submission/file1.txt", files.get(0).getFilePath());
        assertEquals("bucket/study/submission/file2.txt", files.get(1).getFilePath());
        assertEquals("bucket/study/submission/file3.txt", files.get(2).getFilePath());
        loggingEventCaptor.getAllValues().stream().forEach(e -> {
            assertEquals(Level.ERROR, e.getLevel());
        });
    }

    @Test
    void testMoveToApproved_DifferingHash() throws Exception{
        List<S3File> files = getMockS3Files();
        Copy copy = mock(Copy.class);
        CompletableFuture<CompletedCopy> copyFuture = mock(CompletableFuture.class);
        CompletedCopy completedCopy = mock(CompletedCopy.class);
        List<CopyObjectResponse> mockResponses = getMockCopyObjectResponses();

        CompletableFuture<DeleteObjectResponse> deletionFuture = mock(CompletableFuture.class);
        DeleteObjectResponse deleteResponse = DeleteObjectResponse.builder().deleteMarker(true).build();

        when(transferManager.copy(any(CopyRequest.class)))
                .thenReturn(copy);
        when(copy.completionFuture())
                .thenReturn(copyFuture);
        when(copyFuture.join())
                .thenReturn(completedCopy);
        when(completedCopy.response())
                .thenReturn(mockResponses.get(0));
        when(client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(deletionFuture);
        when(deletionFuture.get())
                .thenReturn(deleteResponse);

        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(mockedAppender);
        root.setLevel(Level.INFO);

        boolean response = storageService.moveToApproved(files);

        verify(mockedAppender, times(2)).doAppend(loggingEventCaptor.capture());

        assertTrue(response);
        files.forEach(file -> {
            assertTrue(file.getChecksumHash() != null);
        });
        assertEquals("newBucket/study/file1_v1.txt", files.get(0).getFilePath());
        assertEquals("bucket/study/submission/" + files.get(1).getFileName(), files.get(1).getFilePath());
        assertEquals("bucket/study/submission/" + files.get(2).getFileName(), files.get(2).getFilePath());
        assertEquals(Level.ERROR, loggingEventCaptor.getAllValues().get(0).getLevel());
        assertEquals(Level.ERROR, loggingEventCaptor.getAllValues().get(1).getLevel());
    }
}
