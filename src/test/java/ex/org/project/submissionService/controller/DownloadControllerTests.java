package ex.org.project.submissionService.controller;

import ex.org.project.submissionService.controllers.DownloadController;
import ex.org.project.submissionService.services.DownloadService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DownloadControllerTests {

    public DownloadControllerTests() {
        MockitoAnnotations.initMocks(this);
    }

    @Mock
    private DownloadService downloadService;

    @InjectMocks
    private DownloadController downloadController;

    @Test
    void exportValidationErrorsByFileIdToCSV_shouldCallDownloadService() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        DownloadService downloadService = mock(DownloadService.class);
        Jwt jwt = mock(Jwt.class);

        ReflectionTestUtils.setField(downloadController, "downloadservice", downloadService);

        downloadController.exportValidationErrorsByFileIdToCSV(response, jwt, 1);

        verify(downloadService).getValidationErrors(response, 1);
    }

    @Test
    void exportValidationErrorsBySubmissionToCSV_shouldCallDownloadService() {

        HttpServletResponse response = mock(HttpServletResponse.class);
        DownloadService downloadService = mock(DownloadService.class);
        Jwt jwt = mock(Jwt.class);
        ReflectionTestUtils.setField(downloadController, "downloadservice", downloadService);

        downloadController.exportValidationErrorsBySubmissionToCSV(response, jwt, 1);

        verify(downloadService).getValidationErrorsbySubmission(response, 1);
    }

    @Test
    void exportValidationErrorsByFileIdToCSV_shouldCallDownloadServiceWithNullFileId() {
        // Mock the HttpServletResponse and DownloadService objects
        HttpServletResponse response = mock(HttpServletResponse.class);
        DownloadService downloadService = mock(DownloadService.class);
        Jwt jwt = mock(Jwt.class);

        ReflectionTestUtils.setField(downloadController, "downloadservice", downloadService);

        downloadController.exportValidationErrorsByFileIdToCSV(response, jwt, null);
        verify(downloadService).getValidationErrors(response, null);
    }

    @Test
    void exportValidationErrorsBySubmissionToCSV_shouldCallDownloadServiceWithNullSubmissionId() {
        // Mock the HttpServletResponse and DownloadService objects
        HttpServletResponse response = mock(HttpServletResponse.class);
        DownloadService downloadService = mock(DownloadService.class);
        Jwt jwt = mock(Jwt.class);

        ReflectionTestUtils.setField(downloadController, "downloadservice", downloadService);

        downloadController.exportValidationErrorsBySubmissionToCSV(response, jwt, null);

        verify(downloadService).getValidationErrorsbySubmission(response, null);
    }


}
