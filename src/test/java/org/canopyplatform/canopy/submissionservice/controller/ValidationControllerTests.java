package org.canopyplatform.canopy.submissionservice.controller;

import org.canopyplatform.canopy.submissionservice.auth.UserAuthService;
import org.canopyplatform.canopy.submissionservice.controllers.ValidationController;
import org.canopyplatform.canopy.submissionservice.models.dtos.ValidationResultsDTO;
import org.canopyplatform.canopy.submissionservice.services.BundleService;
import org.canopyplatform.canopy.submissionservice.services.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ValidationControllerTests {

    @Mock
    private ValidationService validationService;

    @Mock
    private BundleService bundleService;

    @Mock
    private UserAuthService authService;

    @InjectMocks
    private ValidationController validationController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testUpdateFileAcknowledgements_AllFilesWithWarningsAcknowledged_SubmitTrue() {
        Jwt jwt = mock(Jwt.class);
        ValidationResultsDTO dto = new ValidationResultsDTO();
        dto.setSubmissionId(1);

        when(authService.checkAuth(anyString(), anyList()))
                .thenReturn(1);
        when(validationService.updateFileAck(dto, 1))
                .thenReturn(true);

        ResponseEntity<Boolean> response = validationController.updateFileAcknowledgements(jwt, dto, true);

        assertEquals(true, response.getBody());
        verify(validationService, times(1)).updateFileAck(dto, 1);
        verify(bundleService, times(1)).updateStepId(dto.getSubmissionId(), "Validate Files", 1);
    }

    @Test
    void testUpdateFileAcknowledgements_AllFilesWithWarningsAcknowledged_SubmitFalse() {
        Jwt jwt = mock(Jwt.class);
        ValidationResultsDTO dto = new ValidationResultsDTO();
        dto.setSubmissionId(1);

        when(authService.checkAuth(anyString(), anyList()))
                .thenReturn(1);
        when(validationService.updateFileAck(dto, 1))
                .thenReturn(false);

        ResponseEntity<Boolean> response = validationController.updateFileAcknowledgements(jwt, dto, false);

        assertEquals(false, response.getBody());
        verify(validationService, times(1)).updateFileAck(dto, 1);
        verify(bundleService, times(0)).updateStepId(anyInt(), anyString(), anyInt());
    }

    @Test
    void testUpdateFileAcknowledgements_NotAllFilesWithWarningsAcknowledged_ReturnsFalse() {
        Jwt jwt = mock(Jwt.class);
        ValidationResultsDTO dto = new ValidationResultsDTO();
        dto.setSubmissionId(1);

        when(authService.checkAuth(anyString(), anyList()))
                .thenReturn(1);
        when(validationService.updateFileAck(dto, 1))
                .thenReturn(false);

        ResponseEntity<Boolean> response = validationController.updateFileAcknowledgements(jwt, dto, true);

        assertEquals(false, response.getBody());
        verify(validationService, times(1)).updateFileAck(dto, 1);
    }

}
