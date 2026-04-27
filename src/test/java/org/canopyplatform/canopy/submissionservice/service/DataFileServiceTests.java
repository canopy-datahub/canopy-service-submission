package org.canopyplatform.canopy.submissionservice.service;

import org.canopyplatform.canopy.submissionservice.exceptions.custom.StatusNotFoundException;
import org.canopyplatform.canopy.submissionservice.exceptions.custom.SubmissionIdInvalidException;
import org.canopyplatform.canopy.submissionservice.mappers.S3FileMapper;
import org.canopyplatform.canopy.submissionservice.mappers.S3FileMapperImpl;
import org.canopyplatform.canopy.submissionservice.mappers.StudyMapper;
import org.canopyplatform.canopy.submissionservice.exceptions.custom.EmptyParameterException;
import org.canopyplatform.canopy.submissionservice.exceptions.custom.StudyNotFoundException;
import org.canopyplatform.canopy.submissionservice.models.*;
import org.canopyplatform.canopy.submissionservice.exceptions.custom.DataFileNotFoundException;

import org.canopyplatform.canopy.submissionservice.models.dtos.S3FileDTO;
import org.canopyplatform.canopy.submissionservice.models.dtos.ValidationResultsDTO;
import org.canopyplatform.canopy.submissionservice.repositories.*;
import org.canopyplatform.canopy.submissionservice.services.AwsStorageService;
import org.canopyplatform.canopy.submissionservice.services.DataFileService;
import org.canopyplatform.canopy.submissionservice.services.ValidationService;
import org.canopyplatform.canopy.submissionservice.services.VariableService;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNull;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataFileServiceTests {

    private StudyMapper studyMapper = Mappers.getMapper(StudyMapper.class);
    private S3FileMapper s3FileMapper = new S3FileMapperImpl();
    private DataFileCategoryRepository dfCategoryRepository = mock(DataFileCategoryRepository.class);
    private DataSubmissionRepository dataSubmissionRepository = mock(DataSubmissionRepository.class);
    private UsersRepository usersRepository = mock(UsersRepository.class);
    private LkupStatusRepository lkupStatusRepository = mock(LkupStatusRepository.class);
    private AwsStorageService awsStorageService = mock(AwsStorageService.class);
    private S3FileRepository s3FileRepository = mock(S3FileRepository.class);
    private StudyRepository studyRepository = mock(StudyRepository.class);
    private StudyPropertyValueRepository studyPropertyValueRepository = mock(StudyPropertyValueRepository.class);

    private VariableRepository variableRepository = mock(VariableRepository.class);

    private SASFileRepository sasFileRepository = mock(SASFileRepository.class);
    private SASFileDownloadRepository sasFileDownloadRepository = mock(SASFileDownloadRepository.class);
    private DataFileDownloadRepository dataFileDownloadRepository = mock(DataFileDownloadRepository.class);
    private DataFileRepository dfRepository = mock(DataFileRepository.class);
    private ValidationService validationService = mock(ValidationService.class);
    private VariableService variableService = mock(VariableService.class);

    private final DataFileService dataFileService = new DataFileService(
            s3FileRepository,
            dfRepository,
            dfCategoryRepository,
            lkupStatusRepository,
            awsStorageService,
            validationService,
            dataSubmissionRepository,
            studyPropertyValueRepository,
            studyRepository,
            usersRepository,
            variableRepository,
            sasFileRepository,
            sasFileDownloadRepository,
            dataFileDownloadRepository,
            studyMapper,
            s3FileMapper,
            variableService
    );

    @Captor
    ArgumentCaptor<Set<Integer>> fileIdSetCaptor;

    @Test
    void testCreateDataFile() {
        // Mock the necessary objects and their behaviors
        S3File s3File = mock(S3File.class);
        DataFileCategory fileCategory = mock(DataFileCategory.class);
        LkupStatus status = mock(LkupStatus.class);
        DataFile dataFile = mock(DataFile.class);

        String fileCategoryName = "Uncategorized";
        Integer submissionId = 25;
        String fileName = "helloWorld.txt";

        // Mock the behavior of the dependencies
        when(s3File.getSubmissionId()).thenReturn(submissionId);
        when(s3File.getFileName()).thenReturn(fileName);
        when(dfCategoryRepository.findByName(fileCategoryName)).thenReturn(fileCategory);
        when(lkupStatusRepository.findByUsageAndName("file", "draft")).thenReturn(Optional.of(status));
        when(dfRepository.saveAndFlush(any(DataFile.class))).thenReturn(dataFile);

        // Call the method to be tested
        DataFile result = dataFileService.createDataFile(s3File, 1);

        // Perform assertions to verify the expected behavior
        verify(s3File).getSubmissionId();
        verify(dfCategoryRepository).findByName(fileCategoryName);
        verify(lkupStatusRepository).findByUsageAndName("file", "draft");
        verify(dfRepository).saveAndFlush(any(DataFile.class));
        Assert.assertEquals(dataFile, result);
    }
    @Test
    void testCreateDataFile_NullStatus() {
        // Mock dependencies
        S3File mockS3File = new S3File();
        mockS3File.setSubmissionId(123);
        mockS3File.setFileName("helloWorld.txt");
        // Mock the repository methods
        DataFileCategory mockFileCategory = new DataFileCategory();
        when(dfCategoryRepository.findByName(anyString())).thenReturn(mockFileCategory);
        when(lkupStatusRepository.findByUsageAndName(anyString(), anyString())).thenThrow(new StatusNotFoundException("Invalid Data File Status: null"));
        assertThrows(StatusNotFoundException.class,
                () -> dataFileService.createDataFile(mockS3File, 1),
                "Invalid Data File Status: null");
    }

    @Test
    void testDeleteDataFile(){
        DataFile mockDataFile = new DataFile();
        mockDataFile.setId(1);
        S3File mockS3File = new S3File();
        mockS3File.setId(8);
        mockS3File.setFilePath("bucket/junit/test.txt");
        mockDataFile.setS3File(mockS3File);

        when(dfRepository.findById(1)).thenReturn(Optional.of(mockDataFile));
        when(s3FileRepository.findById(8)).thenReturn(Optional.of(mockS3File));
        when(dfRepository.findDataFilesByDictionaryFileId(1))
                .thenReturn(new ArrayList<>());
        when(dfRepository.findDataFilesByMetadataFileId(1))
                .thenReturn(new ArrayList<>());
        when(awsStorageService.deleteFileFromS3(mockS3File)).thenReturn(true);

        boolean result = dataFileService.deleteDataFile(1);

        assertEquals(true, result);
        assertEquals("junit/test.txt", mockS3File.getFileKey());
    }

    @Test
    void testDeleteDataFile_FileNotFound(){
        when(dfRepository.findById(anyInt())).thenReturn(Optional.empty());
        try {
            boolean result = dataFileService.deleteDataFile(1);
            fail("Method did not throw expected exception");
        } catch(DataFileNotFoundException e) {
            assertEquals( "No file found with ID 1", e.getMessage());
        }
    }

    @Test
    void testDeleteDataFile_S3FileNotFound(){
        DataFile mockDataFile = new DataFile();
        mockDataFile.setId(1);
        mockDataFile.setS3File(null);
        when(dfRepository.findById(1)).thenReturn(Optional.of(mockDataFile));
        try {
            boolean result = dataFileService.deleteDataFile(1);
            fail("Method did not throw expected exception");
        } catch(DataFileNotFoundException e) {
            assertEquals( "No S3 file found for ID 1", e.getMessage());
        }
    }

    @Test
    void testDeleteDataFile_WithForeignKeyRef(){
        S3File mockS3File = new S3File();
        mockS3File.setId(8);
        mockS3File.setFilePath("bucket/junit/test.txt");

        DataFile mockDataFile = new DataFile();
        mockDataFile.setId(1);
        mockDataFile.setS3File(mockS3File);
        DataFile mockForeignDataFile = new DataFile();
        mockForeignDataFile.setId(2);
        mockForeignDataFile.setDictionaryFileId(1);
        List<DataFile> mockDataFileList = new ArrayList<>();
        mockDataFileList.add(mockForeignDataFile);

        List<Variable> variables = getMockVariables(mockDataFile.getId());

        when(dfRepository.findById(1)).thenReturn(Optional.of(mockDataFile));
        when(dfRepository.findDataFilesByDictionaryFileId(1))
                .thenReturn(mockDataFileList);
        when(awsStorageService.deleteFileFromS3(mockS3File)).thenReturn(true);
        when(variableRepository.findByFileId(1)).thenReturn(variables);

        boolean result = dataFileService.deleteDataFile(1);

        assertEquals(true, result);
        assertEquals("junit/test.txt", mockS3File.getFileKey());
        assertEquals(null, mockForeignDataFile.getDictionaryFileId());
        verify(variableRepository, times(1)).findByFileId(mockDataFile.getId());
        verify(variableRepository, times(1)).deleteAll(anyList());
    }

    @Test
    void testDeleteMultipleFiles(){
        List<Integer> dfIds = List.of(1,2,3);
        List<DataFile> mockDataFiles = getMockDataFiles();
        DataFile df1 = mockDataFiles.get(0);
        DataFile df2 = mockDataFiles.get(1);
        DataFile df3 = mockDataFiles.get(2);

        when(dfRepository.findById(1)).thenReturn(Optional.of(df1));
        when(dfRepository.findById(2)).thenReturn(Optional.of(df2));
        when(dfRepository.findById(3)).thenReturn(Optional.of(df3));
        when(dfRepository.findAllById(dfIds)).thenReturn(mockDataFiles);
        assertEquals(3, dfRepository.findAllById(dfIds).size());

        when(awsStorageService.deleteFileFromS3(df1.getS3File())).thenReturn(true);
        when(awsStorageService.deleteFileFromS3(df2.getS3File())).thenReturn(true);
        when(awsStorageService.deleteFileFromS3(df3.getS3File())).thenReturn(true);

        boolean allDeleted = dataFileService.deleteMultipleDatafiles(dfIds);
        verify(s3FileRepository, times(3)).deleteById(anyInt());
        assertEquals(true, allDeleted);
    }

    @Test
    void testReplaceDataFile(){
        MultipartFile mockFile = mock(MultipartFile.class);

        S3File mockOldS3File = new S3File();
        mockOldS3File.setId(8);
        mockOldS3File.setFilePath("bucket/study/junit/test.csv");
        mockOldS3File.setS3FileKeyAndBucketFromPath();

        S3File mockNewS3File = new S3File();
        mockNewS3File.setId(9);
        mockNewS3File.setFileName("RadxRad_transformBundle_DATA_transformcopy.csv");
        mockNewS3File.setFileSize(123L);
        mockNewS3File.setFilePath("bucket/study/junit/RadxRad_transformBundle_DATA_transformcopy.csv");
        mockNewS3File.setS3FileKeyAndBucketFromPath();

        DataFile mockDataFile = new DataFile();
        mockDataFile.setId(1);
        mockDataFile.setSubmissionId(23);
        mockDataFile.setS3File(mockOldS3File);

        LkupStatus mockLkupStatus = new LkupStatus();
        mockLkupStatus.setId(87);
        mockLkupStatus.setName("draft");
        mockLkupStatus.setUsage("file");

        DataFileCategory mockDataFileCategory = new DataFileCategory(
                3,
                "Tabular Data - Harmonized",
                "data"
        );

        mockDataFile.setFileCategory(mockDataFileCategory);
        when(dfRepository.findById(1)).thenReturn(Optional.of(mockDataFile));
        when(awsStorageService.deleteFileFromS3(mockOldS3File)).thenReturn(true);
        when(awsStorageService.uploadFile(mockFile,23, 1)).thenReturn(mockNewS3File);
        when(lkupStatusRepository.findByUsageAndName("file", "draft")).thenReturn(Optional.of(mockLkupStatus));
        when(dfCategoryRepository.findByName("Tabular Data - Harmonized")).thenReturn(mockDataFileCategory);
        when(dfRepository.saveAndFlush(mockDataFile)).thenReturn(mockDataFile);
        when(validationService.validateIfTransformFile(mockDataFile)).thenReturn(true);
        when(validationService.getFileValidationResults(1)).thenReturn(new ValidationResultsDTO());

        ValidationResultsDTO validationResults = dataFileService.replaceDataFile(1, mockFile, 1);

        assertEquals("study/junit/test.csv", mockOldS3File.getFileKey());
        assertEquals("study/junit/RadxRad_transformBundle_DATA_transformcopy.csv", mockNewS3File.getFileKey());
        assertEquals(mockNewS3File.getFileSize(), mockDataFile.getFileSize());
        assertEquals(mockNewS3File.getFileName(), mockDataFile.getSourceFileName());
        assertEquals("RadxRad_transformBundletransformcopy", mockDataFile.getNormalizedFileName());
        assertEquals(mockDataFileCategory, mockDataFile.getFileCategory());
        assertEquals(mockLkupStatus, mockDataFile.getStatus());
        assertNotNull(validationResults);
    }

    @Test
    void testReplaceDataFile_FileIdNotFound(){
        MultipartFile mockFile = mock(MultipartFile.class);

        when(dfRepository.findById(1)).thenReturn(Optional.empty());

        try {
            ValidationResultsDTO validationResults = dataFileService.replaceDataFile(1, mockFile, 1);
        } catch (DataFileNotFoundException e){
            Assertions.assertTrue(true);
            return;
        }
        Assertions.fail("Method should fail if no DataFile with provided ID is found.");
    }

    private List<DataFileIds> getMockDataFileIds(){
        List<DataFileIds> dfIds = new ArrayList<>(3);
        ProjectionFactory factory = new SpelAwareProxyProjectionFactory();

        DataFileIds df1 = factory.createProjection(DataFileIds.class);
        DataFileIds df2 = factory.createProjection(DataFileIds.class);
        DataFileIds df3 = factory.createProjection(DataFileIds.class);

        df1.setId(1);
        df1.setDictionaryFileId(2);
        df1.setMetadataFileId(3);

        df2.setId(2);

        df3.setId(3);

        dfIds.add(df1);
        dfIds.add(df2);
        dfIds.add(df3);

        return dfIds;
    }

    private List<DataFile> getMockDataFiles(){
        List<DataFile> dfs= new ArrayList<>(3);

        DataFile df1 = new DataFile();
        S3File s3f1 = new S3File();
        s3f1.setId(1);
        s3f1.setFilePath("bucket/path/file1");
        df1.setId(1);
        df1.setS3File(s3f1);

        DataFile df2 = new DataFile();
        S3File s3f2 = new S3File();
        s3f2.setId(2);
        s3f2.setFilePath("bucket/path/file2");
        df2.setId(2);
        df2.setS3File(s3f2);

        DataFile df3 = new DataFile();
        S3File s3f3 = new S3File();
        s3f3.setId(3);
        s3f3.setFilePath("bucket/path/file3");
        df3.setId(3);
        df3.setS3File(s3f3);

        dfs.add(df1);
        dfs.add(df2);
        dfs.add(df3);

        return dfs;
    }

    private List<Variable> getMockVariables(Integer fileId){
        List<Variable> variables = new ArrayList<>(3);

        DataFile mockFile = new DataFile();
        mockFile.setId(fileId);

        Variable variable1 = new Variable();
        variable1.setId(1);
        variable1.setFile(mockFile);
        variable1.setName("nih_record_id");

        Variable variable2 = new Variable();
        variable2.setId(2);
        variable2.setFile(mockFile);
        variable2.setName("study_id");

        Variable variable3 = new Variable();
        variable3.setId(3);
        variable3.setFile(mockFile);
        variable3.setName("participant_id");

        variables.add(variable1);
        variables.add(variable2);
        variables.add(variable3);

        return variables;
    }

    @Test
    void testDeleteBundle_PrimaryFile(){
        List<DataFileIds> dfIdsMock = getMockDataFileIds();
        List<DataFile> dfsMock = getMockDataFiles();
        DataFileIds dfId1 = dfIdsMock.get(0);
        DataFileIds dfId2 = dfIdsMock.get(1);
        DataFileIds dfId3 = dfIdsMock.get(2);
        when(dfRepository.findDistinctById(1))
                .thenReturn(Optional.of(dfId1));
        when(dfRepository.findDataFileByIdInOrDictionaryFileIdInOrMetadataFileIdIn(anySet(), anySet(), anySet()))
                .thenReturn(List.of(dfId1, dfId2, dfId3));
        when(dfRepository.updateForeignKeysToNull(eq(Set.of(1, 2, 3))))
                .thenReturn(3);
        when(dfRepository.findAllById(eq(Set.of(1, 2, 3))))
                .thenReturn(dfsMock);
        when(awsStorageService.deleteFileFromS3(any(S3File.class)))
                .thenReturn(true);

        List<Integer> response = dataFileService.deleteBundle(1);
        verify(dfRepository).findAllById(fileIdSetCaptor.capture());
        Set<Integer> fileIdSetCaptorValue = fileIdSetCaptor.getValue();


        assertEquals(3, fileIdSetCaptorValue.size());
        assertEquals(fileIdSetCaptorValue.size(), response.size());
        response.forEach(id -> assertTrue(fileIdSetCaptorValue.contains(id)));
    }

    @Test
    void testDeleteBundle_ChildFile(){
        List<DataFileIds> dfIdsMock = getMockDataFileIds();
        List<DataFile> dfsMock = getMockDataFiles();
        DataFileIds dfId1 = dfIdsMock.get(0);
        DataFileIds dfId2 = dfIdsMock.get(1);

        when(dfRepository.findDistinctById(2))
                .thenReturn(Optional.of(dfId2));
        when(dfRepository.findDataFileByIdInOrDictionaryFileIdInOrMetadataFileIdIn(anySet(), anySet(), anySet()))
                .thenReturn(List.of(dfId1, dfId2));
        when(dfRepository.updateForeignKeysToNull(eq(Set.of(1, 2, 3))))
                .thenReturn(3);
        when(dfRepository.findAllById(eq(Set.of(1, 2, 3))))
                .thenReturn(dfsMock);
        when(awsStorageService.deleteFileFromS3(any(S3File.class)))
                .thenReturn(true);

        List<Integer> response = dataFileService.deleteBundle(2);
        verify(dfRepository).findAllById(fileIdSetCaptor.capture());
        Set<Integer> fileIdSetCaptorValue = fileIdSetCaptor.getValue();

        assertEquals(3, fileIdSetCaptorValue.size());
        assertEquals(fileIdSetCaptorValue.size(), response.size());
        response.forEach(id -> assertTrue(fileIdSetCaptorValue.contains(id)));
    }

    //This shouldn't ever happen, but it works just in case
    @Test
    void testDeleteBundle_ChildFileHasChildFile(){
        List<DataFileIds> dfIdsMock = getMockDataFileIds();
        DataFileIds dfId1 = dfIdsMock.get(0);
        DataFileIds dfId2 = dfIdsMock.get(1);
        DataFileIds dfId3 = dfIdsMock.get(2);

        ProjectionFactory factory = new SpelAwareProxyProjectionFactory();
        DataFileIds dfId4 = factory.createProjection(DataFileIds.class);
        dfId4.setId(4);
        dfIdsMock.get(1).setDictionaryFileId(4);

        List<DataFile> dfsMock = getMockDataFiles();
        DataFile df4 = new DataFile();
        S3File s3f4 = new S3File();
        s3f4.setId(4);
        s3f4.setFilePath("bucket/path/file4");
        df4.setId(4);
        df4.setS3File(s3f4);
        dfsMock.add(df4);

        when(dfRepository.findDistinctById(1))
                .thenReturn(Optional.of(dfId1));
        when(dfRepository.findDataFileByIdInOrDictionaryFileIdInOrMetadataFileIdIn(anySet(), anySet(), anySet()))
                .thenReturn(List.of(dfId1, dfId2, dfId3));
        when(dfRepository.updateForeignKeysToNull(eq(Set.of(1, 2, 3, 4))))
                .thenReturn(4);
        when(dfRepository.findAllById(eq(Set.of(1, 2, 3, 4))))
                .thenReturn(dfsMock);
        when(awsStorageService.deleteFileFromS3(any(S3File.class)))
                .thenReturn(true);

        List<Integer> response = dataFileService.deleteBundle(1);
        verify(dfRepository).findAllById(fileIdSetCaptor.capture());
        Set<Integer> fileIdSetCaptorValue = fileIdSetCaptor.getValue();

        assertEquals(4, fileIdSetCaptorValue.size());
        assertEquals(fileIdSetCaptorValue.size(), response.size());
        response.forEach(id -> assertTrue(fileIdSetCaptorValue.contains(id)));
    }

    @Test
    void testDeleteBundle_InvalidFileId(){
        when(dfRepository.findDistinctById(1))
                .thenReturn(Optional.empty());
        assertThrows(DataFileNotFoundException.class, () -> dataFileService.deleteBundle(1));
    }

    @Test
    void testDeleteBundle_NullS3File(){
        List<DataFileIds> dfIdsMock = getMockDataFileIds();
        List<DataFile> dfsMock = getMockDataFiles();
        DataFileIds dfId1 = dfIdsMock.get(0);
        DataFileIds dfId2 = dfIdsMock.get(1);
        DataFileIds dfId3 = dfIdsMock.get(2);

        dfsMock.get(0).setS3File(null);

        when(dfRepository.findDistinctById(1))
                .thenReturn(Optional.of(dfId1));
        when(dfRepository.findDataFileByIdInOrDictionaryFileIdInOrMetadataFileIdIn(anySet(), anySet(), anySet()))
                .thenReturn(List.of(dfId1, dfId2, dfId3));
        when(dfRepository.updateForeignKeysToNull(eq(Set.of(1, 2, 3))))
                .thenReturn(3);
        when(dfRepository.findAllById(eq(Set.of(1, 2, 3))))
                .thenReturn(dfsMock);
        when(awsStorageService.deleteFileFromS3(any(S3File.class)))
                .thenReturn(true);

        List<Integer> response = dataFileService.deleteBundle(1);
        verify(dfRepository).findAllById(fileIdSetCaptor.capture());
        Set<Integer> fileIdSetCaptorValue = fileIdSetCaptor.getValue();

        assertEquals(3, fileIdSetCaptorValue.size());
        assertEquals(fileIdSetCaptorValue.size() - 1, response.size());
        response.forEach(id -> assertTrue(fileIdSetCaptorValue.contains(id)));
    }

    @Test
    void testDeleteBundle_S3DeletionError(){
        List<DataFileIds> dfIdsMock = getMockDataFileIds();
        List<DataFile> dfsMock = getMockDataFiles();
        DataFileIds dfId1 = dfIdsMock.get(0);
        DataFileIds dfId2 = dfIdsMock.get(1);
        DataFileIds dfId3 = dfIdsMock.get(2);

        when(dfRepository.findDistinctById(1))
                .thenReturn(Optional.of(dfId1));
        when(dfRepository.findDataFileByIdInOrDictionaryFileIdInOrMetadataFileIdIn(anySet(), anySet(), anySet()))
                .thenReturn(List.of(dfId1, dfId2, dfId3));
        when(dfRepository.updateForeignKeysToNull(eq(Set.of(1, 2, 3))))
                .thenReturn(3);
        when(dfRepository.findAllById(eq(Set.of(1, 2, 3))))
                .thenReturn(dfsMock);
        when(awsStorageService.deleteFileFromS3(dfsMock.get(0).getS3File()))
                .thenReturn(true);
        when(awsStorageService.deleteFileFromS3(dfsMock.get(1).getS3File()))
                .thenReturn(false);
        when(awsStorageService.deleteFileFromS3(dfsMock.get(2).getS3File()))
                .thenReturn(true);

        List<Integer> response = dataFileService.deleteBundle(1);
        verify(dfRepository).findAllById(fileIdSetCaptor.capture());
        Set<Integer> fileIdSetCaptorValue = fileIdSetCaptor.getValue();

        assertEquals(3, fileIdSetCaptorValue.size());
        assertEquals(fileIdSetCaptorValue.size() - 1, response.size());
        response.forEach(id -> assertTrue(fileIdSetCaptorValue.contains(id)));
    }

    @Test
    void testDeleteBundle_DataAccessException(){
        List<DataFileIds> dfIdsMock = getMockDataFileIds();
        List<DataFile> dfsMock = getMockDataFiles();
        DataFileIds dfId1 = dfIdsMock.get(0);
        DataFileIds dfId2 = dfIdsMock.get(1);
        DataFileIds dfId3 = dfIdsMock.get(2);

        when(dfRepository.findDistinctById(1))
                .thenReturn(Optional.of(dfId1));
        when(dfRepository.findDataFileByIdInOrDictionaryFileIdInOrMetadataFileIdIn(anySet(), anySet(), anySet()))
                .thenReturn(List.of(dfId1, dfId2, dfId3));
        when(dfRepository.updateForeignKeysToNull(eq(Set.of(1, 2, 3))))
                .thenReturn(3);
        when(dfRepository.findAllById(eq(Set.of(1, 2, 3))))
                .thenReturn(dfsMock);
        doThrow(new NonTransientDataAccessException("test"){})
                .when(dfRepository).deleteById(1);
        when(awsStorageService.deleteFileFromS3(any(S3File.class)))
                .thenReturn(true);

        List<Integer> response = dataFileService.deleteBundle(1);
        verify(dfRepository).findAllById(fileIdSetCaptor.capture());
        Set<Integer> fileIdSetCaptorValue = fileIdSetCaptor.getValue();

        assertEquals(3, fileIdSetCaptorValue.size());
        assertEquals(fileIdSetCaptorValue.size() - 1, response.size());
        response.forEach(id -> assertTrue(fileIdSetCaptorValue.contains(id)));
    }

    private List<MultipartFile> getMockMultipartFiles(){
        MultipartFile file1 = new MockMultipartFile(
                "file1",
                "hello1.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World 1!".getBytes()
        );
        MultipartFile file2 = new MockMultipartFile(
                "file2",
                "hello2.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World 2!".getBytes()
        );
        MultipartFile file3 = new MockMultipartFile(
                "file3",
                "hello3.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello, World 3!".getBytes()
        );
        return List.of(file1, file2, file3);
    }

    private List<S3File> getS3Files(){
        S3File file1 = new S3File();
        file1.setUploadSuccessful(true);
        file1.setFileName("hello1.txt");
        file1.setSubmissionId(87);

        S3File file2 = new S3File();
        file2.setUploadSuccessful(true);
        file2.setFileName("hello2.txt");
        file2.setSubmissionId(87);

        S3File file3 = new S3File();
        file3.setUploadSuccessful(true);
        file3.setFileName("hello3.txt");
        file3.setSubmissionId(87);

        return List.of(file1, file2, file3);
    }

    private List<DataFile> getDataFiles(){
        DataFile file1 = new DataFile();
        file1.setId(1);

        DataFile file2 = new DataFile();
        file2.setId(2);

        DataFile file3 = new DataFile();
        file3.setId(3);

        return List.of(file1, file2, file3);
    }

    @Test
    void testCreateDataFiles_HappyPath(){
        List<MultipartFile> files = getMockMultipartFiles();
        Integer submissionId = 87;
        Study study = new Study();
        study.setUuid("uuid-12345");
        List<S3File> s3Files = getS3Files();
        DataFileCategory fileCategory = new DataFileCategory();
        LkupStatus status = new LkupStatus();
        List<DataFile> dataFiles = getDataFiles();

        when(dataSubmissionRepository.findById(submissionId))
                .thenReturn(Optional.of(new DataSubmission()));
        when(usersRepository.findById(1))
                .thenReturn(Optional.of(new Users()));
        when(studyRepository.findByDataSubmission_Id(87))
                .thenReturn(study);
        when(awsStorageService.uploadFile(any(), anyInt(), anyString(), anyInt()))
                .thenReturn(s3Files.get(0), s3Files.get(1), s3Files.get(2));
        when(dfCategoryRepository.findByName(anyString()))
                .thenReturn(fileCategory);
        when(lkupStatusRepository.findByUsageAndName("file", "draft"))
                .thenReturn(Optional.of(status));
        when(dfRepository.saveAndFlush(any(DataFile.class)))
                .thenReturn(dataFiles.get(0), dataFiles.get(1), dataFiles.get(2));

        List<S3FileDTO> response = dataFileService.createDataFiles(files, submissionId, 1);

        assertFalse(response.isEmpty());
        assertEquals(3, response.size());
        assertEquals(1, response.get(0).getDataFileId());
        assertEquals(2, response.get(1).getDataFileId());
        assertEquals(3, response.get(2).getDataFileId());

        List<String> filesNames = s3Files.stream().map(S3File::getFileName).toList();
        response.forEach(dto -> filesNames.contains(dto.getFileName()));
        response.forEach(dto -> assertTrue(dto.getUploadSuccessful()));
    }

    @Test
    void testCreateDataFiles_FailedS3Uploads(){
        List<MultipartFile> files = getMockMultipartFiles();
        Integer submissionId = 87;
        Study study = new Study();
        study.setUuid("uuid-12345");
        List<S3File> s3Files = getS3Files();
        s3Files.forEach(file -> file.setUploadSuccessful(false));
        DataFileCategory fileCategory = new DataFileCategory();
        LkupStatus status = new LkupStatus();
        List<DataFile> dataFiles = getDataFiles();

        when(dataSubmissionRepository.findById(submissionId))
                .thenReturn(Optional.of(new DataSubmission()));
        when(studyRepository.findByDataSubmission_Id(87))
                .thenReturn(study);
        when(usersRepository.findById(1))
                .thenReturn(Optional.of(new Users()));
        when(awsStorageService.uploadFile(any(), anyInt(), anyString(), anyInt()))
                .thenReturn(s3Files.get(0), s3Files.get(1), s3Files.get(2));
        when(dfCategoryRepository.findByName(anyString()))
                .thenReturn(fileCategory);
        when(lkupStatusRepository.findByUsageAndName("file", "draft"))
                .thenReturn(Optional.of(status));
        when(dfRepository.saveAndFlush(any(DataFile.class)))
                .thenReturn(dataFiles.get(0), dataFiles.get(1), dataFiles.get(2));

        List<S3FileDTO> response = dataFileService.createDataFiles(files, submissionId, 1);

        assertFalse(response.isEmpty());
        assertEquals(3, response.size());
        assertNull(response.get(0).getDataFileId());
        assertNull(response.get(1).getDataFileId());
        assertNull(response.get(2).getDataFileId());

        List<String> filesNames = s3Files.stream().map(S3File::getFileName).toList();
        response.forEach(dto -> filesNames.contains(dto.getFileName()));
        response.forEach(dto -> assertFalse(dto.getUploadSuccessful()));
    }

    @Test
    void testCreateDataFiles_PartialSuccessS3Upload(){
        List<MultipartFile> files = getMockMultipartFiles();
        Integer submissionId = 87;
        Study study = new Study();
        study.setUuid("uuid-12345");
        List<S3File> s3Files = getS3Files();
        s3Files.get(1).setUploadSuccessful(false);
        DataFileCategory fileCategory = new DataFileCategory();
        LkupStatus status = new LkupStatus();
        List<DataFile> dataFiles = getDataFiles();

        when(studyRepository.findByDataSubmission_Id(87))
                .thenReturn(study);
        when(dataSubmissionRepository.findById(submissionId))
                .thenReturn(Optional.of(new DataSubmission()));
        when(usersRepository.findById(1))
                .thenReturn(Optional.of(new Users()));
        when(awsStorageService.uploadFile(any(), anyInt(), anyString(), anyInt()))
                .thenReturn(s3Files.get(0), s3Files.get(1), s3Files.get(2));
        when(dfCategoryRepository.findByName(anyString()))
                .thenReturn(fileCategory);
        when(lkupStatusRepository.findByUsageAndName("file", "draft"))
                .thenReturn(Optional.of(status));
        when(dfRepository.saveAndFlush(any(DataFile.class)))
                .thenReturn(dataFiles.get(0), dataFiles.get(1), dataFiles.get(2));

        List<S3FileDTO> response = dataFileService.createDataFiles(files, submissionId, 1);

        assertFalse(response.isEmpty());
        assertEquals(3, response.size());
        List<Integer> filtered = response.stream().map(S3FileDTO::getDataFileId).filter(Objects::nonNull).toList();
        assertEquals(response.size()-1, filtered.size());

        List<String> filesNames = s3Files.stream().map(S3File::getFileName).toList();
        response.forEach(dto -> filesNames.contains(dto.getFileName()));
    }

    @Test
    void testCreateDataFiles_StudyNotFound(){
        List<MultipartFile> files = getMockMultipartFiles();
        Integer submissionId = 87;

        when(dataSubmissionRepository.findById(submissionId))
                .thenReturn(Optional.of(new DataSubmission()));
        when(studyRepository.findByDataSubmission_Id(87))
                .thenReturn(null);

        assertThrows(
                StudyNotFoundException.class,
                () -> dataFileService.createDataFiles(files, submissionId, 1)
        );
    }

    @Test
    void testCreateDataFiles_EmptyFileList(){
        List<MultipartFile> files = new ArrayList<>();
        Integer submissionId = 87;

        assertThrows(
                EmptyParameterException.class,
                () -> dataFileService.createDataFiles(files, submissionId, 1)
        );
    }

    @Test
    void testCreateDataFiles_InvalidSubmissionId(){
        List<MultipartFile> files = getMockMultipartFiles();
        Integer submissionId = -87;
        assertThrows(
                SubmissionIdInvalidException.class,
                () -> dataFileService.createDataFiles(files, submissionId, 1)
        );
    }

}
