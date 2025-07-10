package ex.org.project.submissionService.controller;

import ex.org.project.submissionService.auth.AccessRole;
import ex.org.project.submissionService.auth.UserAuthService;
import ex.org.project.submissionService.controllers.StudyRegistrationController;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class StudyRegistrationControllerTests {
    @InjectMocks
    private StudyRegistrationController studyRegistrationController;

    @Mock
    private UserAuthService authService;

    @Mock
    private StudyRegistrationService studyRegistrationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testGetStudiesByDcc() {
        String sessionId = "session123";
        String status = "Pending DCC Input";
        Integer userId = 1;
        List<UserStudyRegistrationDTO> expectedStudies = Arrays.asList(new UserStudyRegistrationDTO(), new UserStudyRegistrationDTO());

        when(authService.checkAuth(sessionId, List.of(AccessRole.DATA_SUBMITTER)))
                .thenReturn(userId);
        when(studyRegistrationService.getUserStudiesByDcc(userId, status))
                .thenReturn(expectedStudies);

        ResponseEntity<?> response = studyRegistrationController.getStudiesByDcc(sessionId,status);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedStudies, response.getBody());

        verify(studyRegistrationService).getUserStudiesByDcc(userId, "Pending DCC Input");
    }

    @Test
    void testGetStudiesByCurator() {
        String sessionId = "session123";
        String status = "Approved";
        List<UserStudyRegistrationDTO> expectedStudies = List.of(
                new UserStudyRegistrationDTO(),
                new UserStudyRegistrationDTO()
        );
        when(studyRegistrationService.getUserStudiesByCurator(status)).thenReturn(expectedStudies);

        ResponseEntity<?> response = studyRegistrationController.getStudiesByCurator(sessionId, status);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedStudies, response.getBody());
    }

}
