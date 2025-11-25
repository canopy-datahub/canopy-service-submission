package ex.org.project.submissionService.controller;

import ex.org.project.datahub.auth.core.KeycloakAuthenticationService;
import ex.org.project.submissionService.controllers.DownloadController;
import ex.org.project.submissionService.services.DownloadService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

public class DownloadControllerTests {

    public DownloadControllerTests() {
        MockitoAnnotations.initMocks(this);
    }

    @Mock
    private DownloadService downloadService;

    @Mock
    private KeycloakAuthenticationService authService;

    @InjectMocks
    private DownloadController downloadController;

    @Test
    void exportValidationErrorsByFileIdToCSV_shouldCallDownloadService() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        Jwt jwt = mock(Jwt.class);
        DownloadService downloadService = mock(DownloadService.class);

        when(authService.checkAuth(any(Jwt.class), anyList())).thenReturn(1);
        ReflectionTestUtils.setField(downloadController, "downloadService", downloadService);

        downloadController.exportValidationErrorsByFileIdToCSV(response, jwt, 1);

        verify(downloadService).getValidationErrors(response, 1);
    }

    @Test
    void exportValidationErrorsBySubmissionToCSV_shouldCallDownloadService() {

        HttpServletResponse response = mock(HttpServletResponse.class);
        Jwt jwt = mock(Jwt.class);
        DownloadService downloadService = mock(DownloadService.class);
        when(authService.checkAuth(any(Jwt.class), anyList())).thenReturn(1);
        ReflectionTestUtils.setField(downloadController, "downloadService", downloadService);

        downloadController.exportValidationErrorsBySubmissionToCSV(response, jwt, 1);

        verify(downloadService).getValidationErrorsbySubmission(response, 1);
    }

    @Test
    void exportValidationErrorsByFileIdToCSV_shouldCallDownloadServiceWithNullFileId() {
        // Mock the HttpServletResponse and DownloadService objects
        HttpServletResponse response = mock(HttpServletResponse.class);
        Jwt jwt = mock(Jwt.class);
        DownloadService downloadService = mock(DownloadService.class);

        when(authService.checkAuth(any(Jwt.class), anyList())).thenReturn(1);
        ReflectionTestUtils.setField(downloadController, "downloadService", downloadService);

        downloadController.exportValidationErrorsByFileIdToCSV(response, jwt, null);
        verify(downloadService).getValidationErrors(response, null);
    }

    @Test
    void exportValidationErrorsBySubmissionToCSV_shouldCallDownloadServiceWithNullSubmissionId() {
        // Mock the HttpServletResponse and DownloadService objects
        HttpServletResponse response = mock(HttpServletResponse.class);
        Jwt jwt = mock(Jwt.class);
        DownloadService downloadService = mock(DownloadService.class);

        when(authService.checkAuth(any(Jwt.class), anyList())).thenReturn(1);
        ReflectionTestUtils.setField(downloadController, "downloadService", downloadService);

        downloadController.exportValidationErrorsBySubmissionToCSV(response, jwt, null);

        verify(downloadService).getValidationErrorsbySubmission(response, null);
    }


}
