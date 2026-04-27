package org.canopyplatform.canopy.submissionservice.controller;

import org.canopyplatform.canopy.submissionservice.auth.AccessRole;
import org.canopyplatform.canopy.submissionservice.auth.core.KeycloakAuthenticationService;
import org.canopyplatform.canopy.submissionservice.controllers.StudyRegistrationController;
import org.canopyplatform.canopy.submissionservice.models.dtos.StudyRegistrationDTO;
import org.canopyplatform.canopy.submissionservice.models.dtos.UserStudyRegistrationDTO;
import org.canopyplatform.canopy.submissionservice.services.StudyRegistrationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class StudyRegistrationControllerTests {
    @InjectMocks
    private StudyRegistrationController studyRegistrationController;

    @Mock
    private KeycloakAuthenticationService authenticationService;

    @Mock
    private StudyRegistrationService studyRegistrationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testGetStudiesByDcc() {
        Jwt jwt = mock(Jwt.class);
        String status = "Pending DCC Input";
        Integer userId = 1;
        List<UserStudyRegistrationDTO> expectedStudies = Arrays.asList(new UserStudyRegistrationDTO(), new UserStudyRegistrationDTO());

        when(authenticationService.checkAuth(jwt, List.of(AccessRole.DATA_SUBMITTER)))
                .thenReturn(userId);
        when(studyRegistrationService.getUserStudiesByCenter(userId, status))
                .thenReturn(expectedStudies);

        ResponseEntity<?> response = studyRegistrationController.getStudiesByCenter(jwt,status);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedStudies, response.getBody());

        verify(studyRegistrationService).getUserStudiesByCenter(userId, "Pending DCC Input");
    }

    @Test
    void testGetStudiesByCurator() {
        Jwt jwt = mock(Jwt.class);
        String status = "Approved";
        List<UserStudyRegistrationDTO> expectedStudies = List.of(
                new UserStudyRegistrationDTO(),
                new UserStudyRegistrationDTO()
        );
        when(studyRegistrationService.getUserStudiesByCurator(status)).thenReturn(expectedStudies);

        ResponseEntity<?> response = studyRegistrationController.getStudiesByCurator(jwt, status);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedStudies, response.getBody());
    }

    @Test
    void testOpenSearchRefreshStudyApproval() {
        StudyRegistrationDTO dto = new StudyRegistrationDTO(any(),any());

        when(studyRegistrationService.editStudyPropertyValues(dto, eq("Curator"), true, anyInt()))
                .thenReturn("Successfully updated property values");

        ResponseEntity<?> response = studyRegistrationController.editStudyAsCurator(any(), any(),true);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(studyRegistrationService, times(1)).triggerOpenSearchRefresh();

    }
}
