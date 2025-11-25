package ex.org.project.submissionService.controller;

import ex.org.project.datahub.auth.core.KeycloakAuthenticationService;
import ex.org.project.datahub.auth.model.AccessRole;
import ex.org.project.submissionService.controllers.StudyRegistrationController;
import ex.org.project.submissionService.models.dtos.StudyRegistrationDTO;
import ex.org.project.submissionService.models.dtos.UserStudyRegistrationDTO;
import ex.org.project.submissionService.services.StudyRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class StudyRegistrationControllerTests {
    @InjectMocks
    private StudyRegistrationController studyRegistrationController;

    @Mock
    private KeycloakAuthenticationService authService;

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

        when(authService.checkAuth(any(Jwt.class), eq(List.of(AccessRole.DATA_SUBMITTER))))
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
        when(authService.checkAuth(any(Jwt.class), anyList())).thenReturn(1);
        when(studyRegistrationService.getUserStudiesByCurator(status)).thenReturn(expectedStudies);

        ResponseEntity<?> response = studyRegistrationController.getStudiesByCurator(jwt, status);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedStudies, response.getBody());
    }

    @Test
    void testOpenSearchRefreshStudyApproval() {
        Jwt jwt = mock(Jwt.class);
        StudyRegistrationDTO dto = new StudyRegistrationDTO(any(),any());
        Integer userId = 1;

        when(authService.checkAuth(any(Jwt.class), anyList())).thenReturn(userId);
        when(studyRegistrationService.editStudyPropertyValues(dto, eq("Curator"), eq(true), eq(userId)))
                .thenReturn("Successfully updated property values");

        ResponseEntity<?> response = studyRegistrationController.editStudyAsCurator(jwt, dto, true);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(studyRegistrationService, times(1)).triggerOpenSearchRefresh();

    }
}
