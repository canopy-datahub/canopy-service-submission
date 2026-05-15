package org.canopyplatform.canopy.submissionservice.controllers;

import org.canopyplatform.canopy.submissionservice.auth.core.KeycloakAuthenticationService;
import org.canopyplatform.canopy.submissionservice.services.DownloadService;
import org.canopyplatform.canopy.submissionservice.services.StudyAccessService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/download")
public class DownloadController {

	private final DownloadService downloadService;
  private final KeycloakAuthenticationService authenticationService;
  private final StudyAccessService studyAccessService;

	@GetMapping("/validationErrorsByFile")
	public void exportValidationErrorsByFileIdToCSV(HttpServletResponse response,
													@AuthenticationPrincipal Jwt jwt,
													@RequestParam("fileId") Integer fileId) {
    authenticationService.checkCapability(jwt, "submission.validation.errors.read");
    studyAccessService.requireSubmitToByFile(jwt, fileId);
		downloadService.getValidationErrors(response, fileId);
	}

	@GetMapping("/validationErrorsBySubmission")
	public void exportValidationErrorsBySubmissionToCSV(HttpServletResponse response,
														@AuthenticationPrincipal Jwt jwt,
														@RequestParam("submissionId") Integer submissionId) {
    authenticationService.checkCapability(jwt, "submission.validation.errors.read");
    studyAccessService.requireStudyManagementBySubmission(jwt, submissionId);
		downloadService.getValidationErrorsbySubmission(response, submissionId);
	}
}
