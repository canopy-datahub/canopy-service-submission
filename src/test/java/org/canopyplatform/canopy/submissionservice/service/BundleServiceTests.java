package org.canopyplatform.canopy.submissionservice.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.*;

import org.canopyplatform.canopy.submissionservice.mappers.*;
import org.canopyplatform.canopy.submissionservice.models.*;
import org.canopyplatform.canopy.submissionservice.models.dtos.BundleDTO;
import org.canopyplatform.canopy.submissionservice.models.dtos.GetBundleFilesDTO;
import org.canopyplatform.canopy.submissionservice.models.dtos.SubmissionBundlesDTO;
import org.canopyplatform.canopy.submissionservice.repositories.*;
import org.canopyplatform.canopy.submissionservice.services.DataFileService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import org.canopyplatform.canopy.submissionservice.exceptions.custom.BadDataException;
import org.canopyplatform.canopy.submissionservice.services.BundleService;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BundleServiceTests {

    @Mock
    private DataFileCategoryRepository dfCategoryRepository;

    @Spy
    private BundleMapper bundleMapper = new BundleMapperImpl();

    @Mock
    private DataFileRepository dfRespository;

    @Mock
    private DataFileService dataFileService;

    @Spy
    private DataFileMapper dataFileMapper = new DataFileMapperImpl();

    @Mock
    private  DataSubmissionRepository dataSubmissionRepository;

    @Mock
    private SubmissionStepMapper submissionInfoMapper;

    @Mock
    private LkupSubmissionStepRepository lkupSubmissionStepRepository;

    @Mock
    private StudyRepository studyRepository;

    @InjectMocks
    private BundleService bundleService;

    private List<DataFile> getMockDataFiles(){
        List<DataFile> dfs= new ArrayList<>(3);

        DataFile df1 = new DataFile();
        S3File s3f1 = new S3File();
        DataFileCategory cat1 = new DataFileCategory();
        cat1.setName("data");
        cat1.setCategoryGroup("data");
        df1.setFileCategory(cat1);
        s3f1.setId(1);
        s3f1.setFilePath("bucket/path/file1");
        df1.setId(1);
        df1.setS3File(s3f1);
        df1.setSourceFileName("sourceName1");
        df1.setMetadataFileId(2);
        df1.setDictionaryFileId(3);
        df1.setSubmissionId(1);

        DataFile df2 = new DataFile();
        S3File s3f2 = new S3File();
        DataFileCategory cat2 = new DataFileCategory();
        cat2.setName("meta");
        cat2.setCategoryGroup("meta");
        df2.setFileCategory(cat2);
        s3f2.setId(2);
        s3f2.setFilePath("bucket/path/file2");
        df2.setId(2);
        df2.setS3File(s3f2);
        df2.setSourceFileName("sourceName2");
        df2.setSubmissionId(1);

        DataFile df3 = new DataFile();
        S3File s3f3 = new S3File();
        DataFileCategory cat3 = new DataFileCategory();
        cat3.setName("dict");
        cat3.setCategoryGroup("dict");
        df3.setFileCategory(cat3);
        s3f3.setId(3);
        s3f3.setFilePath("bucket/path/file3");
        df3.setId(3);
        df3.setS3File(s3f3);
        df3.setSourceFileName("sourceName3");
        df3.setSubmissionId(1);

        DataFile df4 = new DataFile();
        S3File s3f4 = new S3File();
        DataFileCategory cat4 = new DataFileCategory();
        cat4.setName("doc");
        cat4.setCategoryGroup("document");
        df4.setFileCategory(cat4);
        s3f4.setId(4);
        s3f4.setFilePath("bucket/path/file4");
        df4.setId(4);
        df4.setS3File(s3f4);
        df4.setSourceFileName("sourceName4");
        df4.setSubmissionId(1);

        dfs.add(df1);
        dfs.add(df2);
        dfs.add(df3);
        dfs.add(df4);

        return dfs;
    }

    @Test
    void testCreateBundles() {
        Integer submissionId = 5;
        // Create a list of mock DataFiles
        List<DataFile> dataFiles = new ArrayList<>();
        dataFiles.add(new DataFile());

        Study mockStudy = new Study();
        mockStudy.setUuid("ABC-123-456");

        // Mock the behavior of the dataFileRepository
        when(dfRespository.findDataFilesByFileCategory_CategoryGroupAndSubmissionId("data", submissionId))
                .thenReturn(dataFiles);
        when(dataSubmissionRepository.findById(submissionId))
                .thenReturn(Optional.of(new DataSubmission()));

        boolean result = bundleService.createBundles(submissionId);

        assertTrue(result);

    }

    @Test
    void testSetChildReferences_WithAssociatedDictionaryFile() {
        Integer submissionId = 5;
        DataFile dataFile = new DataFile();
        dataFile.setNormalizedFileName("filename");

        DataFile associatedDictionaryFile = new DataFile();
        associatedDictionaryFile.setId(1);

        // Mock the behavior of the dataFileRepository

        when(dfRespository.findDataFileByFileCategory_CategoryGroupAndNormalizedFileNameAndSubmissionId("dictionary","filename",submissionId))
                .thenReturn(associatedDictionaryFile);

        bundleService.setChildReferences(dataFile, submissionId);

        assertEquals(1, dataFile.getDictionaryFileId());
        verify(dfRespository).save(dataFile);
    }

    @Test
    void testSetChildReferences_WithoutAssociatedDictionaryFile() {
        Integer submissionId = 5;
        DataFile dataFile = new DataFile();
        dataFile.setNormalizedFileName("filename");

        // Mock the behavior of the dataFileRepository
        when(dfRespository.findDataFileByFileCategory_CategoryGroupAndNormalizedFileNameAndSubmissionId("dictionary","filename",submissionId))
                .thenReturn(null);

        bundleService.setChildReferences(dataFile, submissionId);

        assertEquals(null, dataFile.getDictionaryFileId());
        verify(dfRespository).save(dataFile);
    }

    @Test
    void updateCategoriesByDTO_ShouldNotUpdateCategoriesAndBundlesWhenSubmissionIdNotFound() {
        // Create a sample DTO object
        SubmissionBundlesDTO dto = new SubmissionBundlesDTO();
        dto.setSubmissionId(5);
        dto.setBundles(new ArrayList<>());
        dto.setUnassigned(new ArrayList<>());
        // Mock the dataFileRepository.findBySubmissionId() method to return null
        Mockito.when(dfRespository.findBySubmissionId(dto.getSubmissionId())).thenReturn(null);

        // Call the method to be tested
        bundleService.updateBundles(dto);

        // Verify that the dataFileRepository.findBySubmissionId() method was called once with the correct argument
        Mockito.verify(dfRespository, Mockito.times(1)).findBySubmissionId(dto.getSubmissionId());
    }

    @Test
    void testUpdateBundles() {
        SubmissionBundlesDTO dto = new SubmissionBundlesDTO();
        dto.setSubmissionId(1);
        dto.setBundles(new ArrayList<>());
        dto.setUnassigned(new ArrayList<>());

        DataFile dataFile = new DataFile();
        dataFile.setId(2);
        DataFileCategory category = new DataFileCategory();
        category.setId(1);
        category.setName("Uncategorized");
        category.setCategoryGroup("other");
        dataFile.setFileCategory(category);

        BundleDTO bundleDTO = new BundleDTO();
        bundleDTO.setId(2);
        bundleDTO.setCategory("document");
        bundleDTO.setChildFiles(new ArrayList<>());

        dto.setDocuments(List.of(bundleDTO));

        DataFileCategory newCategory = new DataFileCategory();
        newCategory.setId(2);
        newCategory.setName("document");

        // Mock the dataFileRepository.saveAll() method
        when(dfRespository.findBySubmissionId(1))
                .thenReturn(List.of(dataFile));
        when(dfRespository.findById(2))
                .thenReturn(Optional.of(dataFile));
        when(dfCategoryRepository.findByName("document"))
                .thenReturn(newCategory);

        // Perform the method under test
        bundleService.updateBundles(dto);

        verify(dfRespository, times(1)).save(any(DataFile.class));
        verify(dfRespository, Mockito.times(1)).saveAll(anyList());
    }

    @Test
    void testUpdateParentDataFiles_ParentWithMoreThanTwoChildren() {
        List<DataFile> bundleEntities = new ArrayList<>();
        List<BundleDTO> bundleDTOs = new ArrayList<>();

        // Create a parent bundle with 3 child files
        BundleDTO parentBundle = new BundleDTO();
        parentBundle.setId(1);
        List<BundleDTO> childFiles = new ArrayList<>();
        childFiles.add(new BundleDTO());
        childFiles.add(new BundleDTO());
        childFiles.add(new BundleDTO());
        parentBundle.setChildFiles((ArrayList<BundleDTO>) childFiles);
        bundleDTOs.add(parentBundle);
        // Assert that it throws a BadDataException with the correct error message
        BadDataException exception = assertThrows(BadDataException.class, () -> bundleService.updateParentDataFiles(bundleEntities, bundleDTOs));
        assertEquals("The data is incorrect: Parent with ID 1 cannot have more than 2 children", exception.getMessage());
        assertEquals(0, bundleEntities.size());
    }

    @Test
    void testGetBundleFiles_LastBundle(){
        Integer fileId = 1;
        Set<Integer> fileIds = Set.of(1, 2, 3);
        List<DataFile> dataFiles = getMockDataFiles();
        dataFiles.remove(3);

        when(dataFileService.getBundleIds(1))
                .thenReturn(fileIds);
        when(dfRespository.findAllById(anySet()))
                .thenReturn(dataFiles);
        when(dfRespository.countAllBySubmissionId(1))
        .thenReturn(3);

        GetBundleFilesDTO response = bundleService.getBundleFiles(fileId);

        assertTrue(response.isLastBundle());
        response.files().forEach(dto -> {
            assertTrue(dataFiles.stream()
                    .map(DataFile::getSourceFileName)
                    .toList()
                    .contains(dto.sourceFileName())
            );
        });
        response.files().forEach(dto -> {
            assertTrue(dataFiles.stream()
                    .map(df -> df.getFileCategory().getName())
                    .toList()
                    .contains(dto.dataFileCategory())
            );
        });
    }

    @Test
    void testGetBundleFiles_NotLastBundle(){
        Integer fileId = 1;
        Set<Integer> fileIds = Set.of(1, 2, 3);
        List<DataFile> dataFiles = getMockDataFiles();

        when(dataFileService.getBundleIds(1))
                .thenReturn(fileIds);
        when(dfRespository.findAllById(anySet()))
                .thenReturn(dataFiles);
        when(dfRespository.countAllBySubmissionId(1))
                .thenReturn(6);

        GetBundleFilesDTO response = bundleService.getBundleFiles(fileId);

        assertFalse(response.isLastBundle());
        response.files().forEach(dto -> {
            assertTrue(dataFiles.stream()
                               .map(DataFile::getSourceFileName)
                               .toList()
                               .contains(dto.sourceFileName())
                      );
        });
        response.files().forEach(dto -> {
            assertTrue(dataFiles.stream()
                               .map(df -> df.getFileCategory().getName())
                               .toList()
                               .contains(dto.dataFileCategory())
                      );
        });
    }

    @Test
    void testUpdateStepIdToupload() {
        Integer submissionId = 6;
        DataSubmission dataSubmission = new DataSubmission();
        LkupSubmissionStep step = new LkupSubmissionStep();
        step.setId(1);
        step.setDescription("Upload Files");
        dataSubmission.setStepId(step);

        LkupSubmissionStep lkupSubmissionStep = new LkupSubmissionStep();
        lkupSubmissionStep.setId(2);

        Mockito.when(dataSubmissionRepository.findById(submissionId))
                .thenReturn(Optional.of(dataSubmission));
        when(lkupSubmissionStepRepository.findByDescription("Upload Files")).thenReturn(Optional.of(lkupSubmissionStep));

        bundleService.goBackAndUploadFiles(submissionId);

        verify(dataSubmissionRepository, times(1)).save(any(DataSubmission.class));
    }

    @Test
    void testUpdateStepIdToUpload_WhenLookupStepNotFound() {
        // Arrange
        Integer submissionId = 6;
        DataSubmission dataSubmission = new DataSubmission();
        Mockito.when(dataSubmissionRepository.findById(submissionId))
                .thenReturn(Optional.of(dataSubmission));
        Mockito.when(lkupSubmissionStepRepository.findByDescription("Upload Files"))
                .thenReturn(Optional.empty());

        // Act and Assert
        Assertions.assertThrows(NoSuchElementException.class, () -> bundleService.goBackAndUploadFiles(submissionId));
    }

    @Test
    void testGetBundles_HappyPath(){
        Integer submissionId = 1;
        List<DataFile> dataFiles = getMockDataFiles();
        DataSubmission submission = new DataSubmission();
        submission.setStudyId(1);

        when(dfRespository.findBySubmissionId(1))
                .thenReturn(dataFiles);
        when(dataSubmissionRepository.findById(anyInt()))
                .thenReturn(Optional.of(submission));

        SubmissionBundlesDTO response = bundleService.getBundles(submissionId);

        assertEquals("sourceName1", response.getBundles().get(0).getName());
        assertEquals(2, response.getBundles().get(0).getChildFiles().size());
        assertEquals(1, response.getDocuments().size());
        assertTrue(response.getUnassigned().isEmpty());
    }

    @Test
    void testGetBundles_UnassignedFile(){
        Integer submissionId = 1;
        List<DataFile> dataFiles = getMockDataFiles();
        dataFiles.get(0).setDictionaryFileId(null);
        DataSubmission submission = new DataSubmission();
        submission.setStudyId(1);

        when(dfRespository.findBySubmissionId(1))
                .thenReturn(dataFiles);
        when(dataSubmissionRepository.findById(anyInt()))
                .thenReturn(Optional.of(submission));

        SubmissionBundlesDTO response = bundleService.getBundles(submissionId);

        assertEquals("sourceName1", response.getBundles().get(0).getName());
        assertEquals(1, response.getBundles().get(0).getChildFiles().size());
        assertTrue(response.getUnassigned().size() == 1);
    }
}



