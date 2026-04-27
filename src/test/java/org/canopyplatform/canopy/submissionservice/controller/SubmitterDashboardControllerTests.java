package org.canopyplatform.canopy.submissionservice.controller;

import org.canopyplatform.canopy.submissionservice.auth.core.KeycloakAuthenticationService;
import org.canopyplatform.canopy.submissionservice.controllers.SubmitterDashboardController;
import org.canopyplatform.canopy.submissionservice.models.dtos.SubmissionInfoDTO;
import org.canopyplatform.canopy.submissionservice.services.SubmitterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

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
    private KeycloakAuthenticationService authenticationService;

    @InjectMocks
    private SubmitterDashboardController submitterDashboardController;

    @Test
    void testGetSubmissions() {
        Jwt jwt = mock(Jwt.class);
        String status = "in_progress";
        List<SubmissionInfoDTO> submissions = new ArrayList<>();

        when(submitterService.getSubmissions(anyInt(), anyString()))
                .thenReturn(submissions);
        when(authenticationService.checkAuth(any(Jwt.class), anyList()))
                .thenReturn(1);

        ResponseEntity<?> response = submitterDashboardController.getSubmissions(jwt, status);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(submissions, response.getBody());
        verify(submitterService).getSubmissions(1, "in_progress");
    }

    @Test
    void testGetSubmissions_whenNoSubmissionsFound() {
        Jwt jwt = mock(Jwt.class);
        String status = "in_progress";
        when(submitterService.getSubmissions(anyInt(), anyString()))
                .thenReturn(Collections.emptyList());
        when(authenticationService.checkAuth(any(Jwt.class), anyList()))
                .thenReturn(1);

        ResponseEntity<?> response = submitterDashboardController.getSubmissions(jwt, status);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Collections.emptyList(), response.getBody());
        verify(submitterService).getSubmissions(1, "in_progress");
    }

    @Test
    void deleteSubmission_shouldDeleteSubmissionAndReturnOk() {
        Jwt jwt = mock(Jwt.class);
        Integer submissionId = 1;
        submitterDashboardController.deleteSubmission(jwt, submissionId);
        verify(submitterService, times(1)).deleteSubmission(submissionId);
    }

    @Test
    void deleteSubmission_shouldHandleExceptionAndReturnInternalServerError() {
        // Setup
        Jwt jwt = mock(Jwt.class);
        Integer submissionId = 1;
        doThrow(new RuntimeException("Failed to delete submission")).when(submitterService)
                .deleteSubmission(submissionId);
        try {
            submitterDashboardController.deleteSubmission(jwt, submissionId);

            fail("Expected exception to be thrown");
        } catch (RuntimeException ex) {
            assertEquals("Failed to delete submission", ex.getMessage());
            verify(submitterService, times(1)).deleteSubmission(submissionId);
        }
    }

}


