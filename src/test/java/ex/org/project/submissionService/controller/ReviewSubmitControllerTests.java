package ex.org.project.submissionService.controller;

import ex.org.project.datahub.auth.core.KeycloakAuthenticationService;
import ex.org.project.submissionService.controllers.ReviewSubmitController;
import ex.org.project.submissionService.exceptions.custom.BadDataException;
import ex.org.project.submissionService.services.BundleService;
import ex.org.project.submissionService.services.SubmissionService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.NoSuchElementException;

import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ReviewSubmitControllerTests {
    @Mock
    private SubmissionService submitService;

    @Mock
    private BundleService bundleService;

    @Mock
    private KeycloakAuthenticationService authService;

    @InjectMocks
    private ReviewSubmitController submitController;

    public ReviewSubmitControllerTests() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSubmit_SuccessfulSubmission() throws BadDataException {
        Integer submissionId = 1;
        Integer userId = 1;
        String stepDescription = "Submitted";
        Jwt jwt = mock(Jwt.class);
        when(authService.checkAuth(any(Jwt.class), anyList())).thenReturn(userId);
        ResponseEntity<String> response = submitController.submit(jwt, submissionId);
        doNothing().when(bundleService).updateStepId(submissionId, stepDescription, userId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(submitService, times(1)).submit(submissionId);
        verify(bundleService, times(1)).updateStepId(submissionId, stepDescription, userId);
    }
    @Test
    public void testSubmit_BadDataException() throws BadDataException {
        Integer submissionId = 1;
        Integer userId = 1;
        String errorMessage = "Invalid data";
        Jwt jwt = mock(Jwt.class);
        when(authService.checkAuth(any(Jwt.class), anyList())).thenReturn(userId);
        doThrow(new NoSuchElementException(errorMessage)).when(bundleService).updateStepId(submissionId, "Submitted", userId);
        ResponseEntity<String> response = submitController.submit(jwt, submissionId);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(errorMessage, response.getBody());
        verify(submitService, times(1)).submit(submissionId);
        verify(bundleService, times(1)).updateStepId(submissionId, "Submitted", userId);
    }
}
