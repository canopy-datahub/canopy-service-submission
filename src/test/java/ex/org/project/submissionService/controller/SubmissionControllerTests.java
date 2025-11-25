package ex.org.project.submissionService.controller;

import ex.org.project.datahub.auth.core.KeycloakAuthenticationService;
import ex.org.project.datahub.auth.model.AccessRole;
import ex.org.project.submissionService.controllers.SubmissionController;
import ex.org.project.submissionService.models.dtos.StudiesDTO;
import ex.org.project.submissionService.services.BundleService;
import ex.org.project.submissionService.services.DataFileService;
import ex.org.project.submissionService.services.SubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class SubmissionControllerTests {

    @Mock
    private SubmissionService studyService;

	@Mock
	private BundleService bundleService;

    @Mock
    private  DataFileService datafileService;

	@Mock
	private KeycloakAuthenticationService authService;

	@InjectMocks
	private SubmissionController submissionController;

    @BeforeEach
    void setup() {
        studyService = mock(SubmissionService.class);
		submissionController = new SubmissionController(studyService, datafileService, bundleService, authService);
    }

	@Test
	public void testGetStudiesByDccName() {
		Jwt jwt = mock(Jwt.class);
		List<StudiesDTO> studiesList = new ArrayList<>();
		Integer userId = 1;

		// Mock the behavior of the studyService.getStudyPropertyValues() method to
		// return the studiesList.
		when(authService.checkAuth(any(Jwt.class), eq(List.of(AccessRole.DATA_SUBMITTER))))
				.thenReturn(userId);
		when(studyService.getStudiesByUserCenter(eq(userId)))
				.thenReturn(studiesList);

		// Call the getStudiesByDccName() method of the controller and store the result.
		ResponseEntity<List<StudiesDTO>> response = submissionController.getStudiesByUserCenter(jwt);

		assertNotNull(response);
		assertEquals(studiesList, response.getBody());
	}


}




