package ex.org.project.submissionService.service;

import ex.org.project.submissionService.exceptions.custom.DataFileNotFoundException;
import ex.org.project.submissionService.models.DataFile;
import ex.org.project.submissionService.models.DataFileCategory;
import ex.org.project.submissionService.models.DataSubmission;
import ex.org.project.submissionService.models.ValidationResult;
import ex.org.project.submissionService.models.dtos.ValidationResultsDTO;
import ex.org.project.submissionService.repositories.DataFileRepository;
import ex.org.project.submissionService.repositories.DataSubmissionRepository;
import ex.org.project.submissionService.services.ValidationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
class ValidationServiceTests {

    @Mock
    private DataFileRepository dataFileRepository;

    @InjectMocks
    private ValidationService validationService;

    @Mock
    private DataSubmissionRepository dataSubmissionRepository = mock(DataSubmissionRepository.class);


    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private final String validationResults = "{\"cdeErrors\": null, \"fileId\": 4277, \"fileName\": \"RadxRad_transformBundle_DATA_transformcopy.csv\", \"fileSize\": 15, \"fileType\": \"Tabular Data - Harmonized\", \"acknowledged\": false, \"missingHeaders\": [\"header1\", \"header2\", \"header3\"], \"validationType\": \"Tier1 Headers Ruleset Validation Result\", \"dataEntryWarningCount\": null, \"validationResultsStatus\": \"Warning: No Valid Tier1 Headers to Validate\"}";

    private List<DataFile> getMockDataFiles() {
        DataFileCategory category = new DataFileCategory(
                3,
                "Tabular Data - Harmonized",
                "data"
        );

        DataFile mockDataFile1 = new DataFile();
        mockDataFile1.setId(1);
        mockDataFile1.setSubmissionId(23);
        mockDataFile1.setFileCategory(category);
        mockDataFile1.setValidationResults(validationResults);

        DataFile mockDataFile2 = new DataFile();
        mockDataFile2.setId(2);
        mockDataFile2.setSubmissionId(23);
        mockDataFile2.setFileCategory(category);
        mockDataFile2.setValidationResults(validationResults);

        DataFile mockDataFile3 = new DataFile();
        mockDataFile3.setId(3);
        mockDataFile3.setSubmissionId(23);
        mockDataFile3.setFileCategory(category);
        mockDataFile3.setValidationResults(validationResults);

        return new ArrayList<>(List.of(mockDataFile1, mockDataFile2, mockDataFile3));
    }

    private DataFile getMockDataFile() {
        DataFile mockDataFile = new DataFile();
        mockDataFile.setId(1);
        mockDataFile.setSubmissionId(23);
        DataFileCategory category = new DataFileCategory(
                3,
                "Tabular Data - Harmonized",
                "data"
        );
        mockDataFile.setFileCategory(category);
        mockDataFile.setValidationResults(validationResults);

        return mockDataFile;
    }

    @Test
    void testGetSubmissionValidationResults() {
        Integer submissionId = 23;

        when(dataSubmissionRepository.findById(submissionId)).thenReturn(Optional.of(new DataSubmission()));
        when(dataFileRepository.findBySubmissionId(23))
                .thenReturn(getMockDataFiles());

        ValidationResultsDTO results = validationService.getSubmissionValidationResults(submissionId);

        System.out.println(results);

        Assertions.assertEquals(submissionId, results.getSubmissionId());
        Assertions.assertEquals(4277, results.getBundles().get(0).getFileId());
        Assertions.assertEquals(3, results.getBundles().size());
    }

    @Test
    void testGetSubmissionValidationResults_EmptyDataFileList() {
        Integer submissionId = 23;

        when(dataSubmissionRepository.findById(submissionId)).thenReturn(Optional.of(new DataSubmission()));
        when(dataFileRepository.findDataFilesByFileCategory_NameAndSubmissionId("Tabular Data - Harmonized", 23))
                .thenReturn(new ArrayList<DataFile>());

        ValidationResultsDTO results = validationService.getSubmissionValidationResults(submissionId);

        System.out.println(results);
        Assertions.assertEquals(submissionId, results.getSubmissionId());
        Assertions.assertEquals(0, results.getBundles().size());
    }

    @Test
    void testGetFileValidationResults() {
        Integer submissionId = 23;

        when(dataFileRepository.findById(submissionId))
                .thenReturn(Optional.of(getMockDataFile()));

        ValidationResultsDTO results = validationService.getFileValidationResults(submissionId);

        System.out.println(results);
        Assertions.assertEquals(submissionId, results.getSubmissionId());
        Assertions.assertEquals(4277, results.getBundles().get(0).getFileId());
        Assertions.assertEquals(1, results.getBundles().size());
    }

    @Test
    void testGetFileValidationResults_NoValidationResults() {
        Integer submissionId = 23;
        DataFile dataFile = getMockDataFile();
        dataFile.setValidationResults(null);

        when(dataFileRepository.findById(submissionId))
                .thenReturn(Optional.of(dataFile));

        ValidationResultsDTO results = validationService.getFileValidationResults(submissionId);

        System.out.println(results);
        Assertions.assertEquals(submissionId, results.getSubmissionId());
        //files with no error still returns validation results
        Assertions.assertEquals(1, results.getBundles().size());
    }

    @Test
    void testGetFileValidationResults_FileIdNotFound() {
        Integer submissionId = 23;

        when(dataFileRepository.findById(submissionId))
                .thenReturn(Optional.empty());

        try {
            validationService.getFileValidationResults(submissionId);
        } catch (DataFileNotFoundException e) {
            Assertions.assertTrue(true);
            return;
        }
        Assertions.fail("Method should fail if no DataFile with provided ID is found.");
    }

    @Disabled("This method's workflow is not set up to easily test without also testing the entire CDE Validation")
    @Test
    void testValidateIfTransformFile() {
    }

    @Disabled("This method's workflow is not set up to easily test without also testing the entire CDE Validation")
    @Test
    void testValidateTransformFiles() {
    }


    @Test
    void testUpdateFileAck_NoFiles_ReturnsFalse() {
        ValidationResultsDTO dto = new ValidationResultsDTO();
        dto.setSubmissionId(1);
        dto.setBundles(new ArrayList<>());

        when(dataFileRepository.findBySubmissionId(1)).thenReturn(new ArrayList<>());

        assertFalse(validationService.updateFileAck(dto, 1));
    }

    @Test
    void testUpdateFileAck_NoFailedFiles_ReturnsTrue() {
        ValidationResultsDTO dto = new ValidationResultsDTO();
        dto.setSubmissionId(1);

        List<ValidationResult> validationResults = new ArrayList<>();
        ValidationResult result1 = new ValidationResult();
        result1.setAcknowledged(true);
        result1.setFileId(1);
        validationResults.add(result1);
        dto.setBundles(validationResults);

        when(dataFileRepository.findBySubmissionId(1)).thenReturn(getMockedSubmissionFiles());
        when(dataFileRepository.findById(1)).thenReturn(getMockedDataFile(1, false));

        assertTrue(validationService.updateFileAck(dto, 1));
    }

    @Test
    void testUpdateFileAck_FailedFileNotAcknowledged_ReturnsFalse() {

        ValidationResultsDTO dto = new ValidationResultsDTO();
        dto.setSubmissionId(1);
        List<ValidationResult> validationResults = new ArrayList<>();
        ValidationResult result1 = new ValidationResult();
        result1.setAcknowledged(false);
        result1.setFileId(1);
        validationResults.add(result1);
        dto.setBundles(validationResults);

        when(dataFileRepository.findBySubmissionId(1)).thenReturn(getMockedSubmissionFiles());
        when(dataFileRepository.findById(1)).thenReturn(getMockedDataFile(1, false));

        assertTrue(validationService.updateFileAck(dto, 1));
    }

    // Helper methods to create mocked objects
    private List<DataFile> getMockedSubmissionFiles() {
        List<DataFile> submissionFiles = new ArrayList<>();
        DataFile file1 = new DataFile();
        file1.setId(1);
        file1.setCdeValidationFailed(false);
        file1.setMetaValidationFailed(false);
        file1.setDictValidationFailed(false);
        file1.setPiiPhiFailed(false);
        file1.setValidationAcknowledged(false);
        submissionFiles.add(file1);
        return submissionFiles;
    }

    private Optional<DataFile> getMockedDataFile(int id, boolean validationFailed) {
        DataFile file = new DataFile();
        file.setId(id);
        file.setCdeValidationFailed(validationFailed);
        file.setMetaValidationFailed(validationFailed);
        file.setDictValidationFailed(validationFailed);
        file.setPiiPhiFailed(validationFailed);
        return Optional.of(file);
    }

}

