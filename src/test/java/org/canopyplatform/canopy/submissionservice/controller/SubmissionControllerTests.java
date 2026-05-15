package org.canopyplatform.canopy.submissionservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.canopyplatform.canopy.submissionservice.auth.AccessRole;
import org.canopyplatform.canopy.submissionservice.auth.core.KeycloakAuthenticationService;
import org.canopyplatform.canopy.submissionservice.services.*;
import org.canopyplatform.canopy.submissionservice.services.BundleService;
import org.canopyplatform.canopy.submissionservice.services.DataFileService;
import org.canopyplatform.canopy.submissionservice.services.SubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.canopyplatform.canopy.submissionservice.controllers.SubmissionController;
import org.canopyplatform.canopy.submissionservice.models.dtos.StudiesDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
public class SubmissionControllerTests {

    @Mock
    private SubmissionService studyService;

	@Mock
	private BundleService bundleService;

    @Mock
    private DataFileService datafileService;

	@Mock
  private KeycloakAuthenticationService authenticationService;

	@Mock
	private StudyAccessService studyAccessService;

	@InjectMocks
	private SubmissionController submissionController;

    @BeforeEach
    void setup() {
        studyService = mock(SubmissionService.class);
		submissionController = new SubmissionController(studyService, datafileService, bundleService, authenticationService, studyAccessService);
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




