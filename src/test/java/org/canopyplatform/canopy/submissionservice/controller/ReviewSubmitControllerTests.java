package org.canopyplatform.canopy.submissionservice.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.canopyplatform.canopy.submissionservice.auth.core.KeycloakAuthenticationService;
import org.canopyplatform.canopy.submissionservice.controllers.ReviewSubmitController;
import org.canopyplatform.canopy.submissionservice.exceptions.custom.BadDataException;
import org.canopyplatform.canopy.submissionservice.services.BundleService;
import org.canopyplatform.canopy.submissionservice.services.SubmissionService;

public class ReviewSubmitControllerTests {
    @Mock
    private SubmissionService submitService;

    @Mock
    private BundleService bundleService;

    @Mock
    private KeycloakAuthenticationService authenticationService;

    @InjectMocks
    private ReviewSubmitController submitController;

    public ReviewSubmitControllerTests() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSubmit_SuccessfulSubmission() throws BadDataException {
        Integer submissionId = 1;
        String stepDescription = "Submitted";
        Jwt jwt = mock(Jwt.class);
        ResponseEntity<String> response = submitController.submit(jwt, submissionId);
        doNothing().when(bundleService).updateStepId(submissionId, stepDescription, 0);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(submitService, times(1)).submit(submissionId);
        verify(bundleService, times(1)).updateStepId(submissionId, stepDescription, 0);
    }
    @Test
    public void testSubmit_BadDataException() throws BadDataException {
        Integer submissionId = 1;
        String errorMessage = "Invalid data";
        Jwt jwt = mock(Jwt.class);
        doThrow(new NoSuchElementException(errorMessage)).when(bundleService).updateStepId(submissionId, "Submitted", 0);
        ResponseEntity<String> response = submitController.submit(jwt, submissionId);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(errorMessage, response.getBody());
        verify(submitService, times(1)).submit(submissionId);
        verify(bundleService, times(1)).updateStepId(submissionId, "Submitted", 0);
    }
}
