package ex.org.project.submissionService.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import ex.org.project.submissionService.auth.AccessRole;
import ex.org.project.submissionService.auth.UserAuthService;
import ex.org.project.submissionService.auth.core.KeycloakAuthenticationService;
import ex.org.project.submissionService.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ex.org.project.submissionService.controllers.SubmissionController;
import ex.org.project.submissionService.models.dtos.StudiesDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
public class SubmissionControllerTests {

    @Mock
    private SubmissionService studyService;

	@Mock
	private BundleService bundleService;

    @Mock
    private  DataFileService datafileService;

	@Mock
  private KeycloakAuthenticationService authenticationService;

	@InjectMocks
	private SubmissionController submissionController;

    @BeforeEach
    void setup() {
        studyService = mock(SubmissionService.class);
		submissionController = new SubmissionController(studyService, datafileService, bundleService, authenticationService);
    }

	@Test
	public void testGetStudiesByDccName() {
    Jwt jwt = mock(Jwt.class);
		List<StudiesDTO> studiesList = new ArrayList<>();
		Integer userId = 1;

		// Mock the behavior of the studyService.getStudyPropertyValues() method to
		// return the studiesList.
		when(authenticationService.checkAuth(eq(jwt), eq(List.of(AccessRole.DATA_SUBMITTER))))
				.thenReturn(userId);
		when(studyService.getStudiesByUserCenter(eq(userId)))
				.thenReturn(studiesList);

		// Call the getStudiesByDccName() method of the controller and store the result.
		ResponseEntity<List<StudiesDTO>> response = submissionController.getStudiesByUserCenter(jwt);

		assertNotNull(response);
		assertEquals(studiesList, response.getBody());
	}


}




