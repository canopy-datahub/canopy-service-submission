package ex.org.project.submissionService.controller;

import ex.org.project.submissionService.auth.UserAuthService;
import ex.org.project.submissionService.controllers.SubmitterDashboardController;
import ex.org.project.submissionService.models.dtos.SubmissionInfoDTO;
import ex.org.project.submissionService.services.SubmitterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.aspectj.bridge.MessageUtil.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubmitterDashboardControllerTests {

    @Mock
    private SubmitterService submitterService;

    @Mock
    private UserAuthService authService;

    @InjectMocks
    private SubmitterDashboardController submitterDashboardController;

    @Test
    void testGetSubmissions() {
        String sessionId = "session123";
        String status = "in_progress";
        List<SubmissionInfoDTO> submissions = new ArrayList<>();

        when(submitterService.getSubmissions(anyInt(), anyString()))
                .thenReturn(submissions);
        when(authService.checkAuth(anyString(), anyList()))
                .thenReturn(1);

        ResponseEntity<?> response = submitterDashboardController.getSubmissions(sessionId, status);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(submissions, response.getBody());
        verify(submitterService).getSubmissions(1, "in_progress");
    }

    @Test
    void testGetSubmissions_whenNoSubmissionsFound() {
        String sessionId = "session123";
        String status = "in_progress";
        when(submitterService.getSubmissions(anyInt(), anyString()))
                .thenReturn(Collections.emptyList());
        when(authService.checkAuth(anyString(), anyList()))
                .thenReturn(1);

        ResponseEntity<?> response = submitterDashboardController.getSubmissions(sessionId, status);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Collections.emptyList(), response.getBody());
        verify(submitterService).getSubmissions(1, "in_progress");
    }

    @Test
    void deleteSubmission_shouldDeleteSubmissionAndReturnOk() {
        String sessionId = "session123";
        Integer submissionId = 1;
        submitterDashboardController.deleteSubmission(sessionId, submissionId);
        verify(submitterService, times(1)).deleteSubmission(submissionId);
    }

    @Test
    void deleteSubmission_shouldHandleExceptionAndReturnInternalServerError() {
        // Setup
        String sessionId = "session123";
        Integer submissionId = 1;
        doThrow(new RuntimeException("Failed to delete submission")).when(submitterService)
                .deleteSubmission(submissionId);
        try {
            submitterDashboardController.deleteSubmission(sessionId, submissionId);

            fail("Expected exception to be thrown");
        } catch (RuntimeException ex) {
            assertEquals("Failed to delete submission", ex.getMessage());
            verify(submitterService, times(1)).deleteSubmission(submissionId);
        }
    }

}


