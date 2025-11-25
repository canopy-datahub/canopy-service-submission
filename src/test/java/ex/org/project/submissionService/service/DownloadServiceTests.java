
package ex.org.project.submissionService.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ex.org.project.submissionService.models.DataFile;

import ex.org.project.submissionService.models.ValidationResult;
import ex.org.project.submissionService.repositories.DataFileRepository;
import ex.org.project.submissionService.repositories.DataSubmissionRepository;
import ex.org.project.submissionService.services.DownloadService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.util.*;

public class DownloadServiceTests {
    @Mock
    private DataFileRepository dataFileRepository;
    private DataSubmissionRepository dataSubmissionRepository = mock(DataSubmissionRepository.class);
    @InjectMocks
    private DownloadService downloadService;
    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetValidationErrors() throws Exception {
        // Create a mock HttpServletResponse
        MockHttpServletResponse response = new MockHttpServletResponse();

        DataFileRepository dataFileRepository = mock(DataFileRepository.class);

        DataFile dataFile = mock(DataFile.class);
        when(dataFile.getValidationResults()).thenReturn("{\"fileName\":\"test.csv\",\"fileType\":\"csv\"}");

        ValidationResult validationResult = mock(ValidationResult.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.readValue("{\"fileName\":\"test.csv\",\"fileType\":\"csv\"}", ValidationResult.class))
                .thenReturn(validationResult);

        DownloadService downloadservice = new DownloadService(dataFileRepository,dataSubmissionRepository);

        when(dataFileRepository.findById(1)).thenReturn(Optional.of(dataFile));

        downloadservice.getValidationErrors(response, 1);

        assertEquals("text/csv", response.getContentType());
        assertEquals("attachment; filename=validation_errors.csv", response.getHeader("Content-Disposition"));

        assertNotEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
    }

    @Test
    void testGetValidationByErrorsSubmission() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        List<DataFile> dataFiles = new ArrayList<>();
        DataFile dataFile1 = new DataFile();
        DataFile dataFile2 = new DataFile();
        dataFiles.add(dataFile1);
        dataFiles.add(dataFile2);

        DataFileRepository dataFileRepository = mock(DataFileRepository.class);

        DataFile dataFile = mock(DataFile.class);
        when(dataFile.getValidationResults()).thenReturn("{\"fileName\":\"test.csv\",\"fileType\":\"csv\"}");

        ValidationResult validationResult = mock(ValidationResult.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.readValue("{\"fileName\":\"test.csv\",\"fileType\":\"csv\"}", ValidationResult.class))
                .thenReturn(validationResult);

        DownloadService downloadservice = new DownloadService(dataFileRepository,dataSubmissionRepository);

        when(dataFileRepository.findBySubmissionId(5)).thenReturn(dataFiles);

        downloadservice.getValidationErrors(response, 1);
        assertNotEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
    }

    @Test
    void testGetValidationErrorsBySubmissionException() throws IOException {
        when(dataFileRepository.findBySubmissionId(1)).thenThrow(new RuntimeException("Repository Exception"));
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        downloadService.getValidationErrors(mockResponse, 12);
        assertEquals(HttpServletResponse.SC_OK, mockResponse.getStatus());
        verify(dataFileRepository, never()).findBySubmissionId(anyInt());
    }


}
