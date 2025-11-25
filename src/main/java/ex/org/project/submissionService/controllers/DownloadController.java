package ex.org.project.submissionService.controllers;

import ex.org.project.datahub.auth.core.KeycloakAuthenticationService;
import ex.org.project.datahub.auth.model.AccessRole;
import ex.org.project.submissionService.services.DownloadService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@RestController
@RequiredArgsConstructor
@RequestMapping("/download")
public class DownloadController {

	private final DownloadService downloadService;
	private final KeycloakAuthenticationService authenticationService;

	@GetMapping("/validationErrorsByFile")
	public void exportValidationErrorsByFileIdToCSV(HttpServletResponse response,
													@AuthenticationPrincipal Jwt jwt,
													@RequestParam("fileId") Integer fileId) {
		//TODO: file authorization
		authenticationService.checkAuth(jwt, List.of(AccessRole.DATA_CURATOR, AccessRole.DATA_SUBMITTER));
		downloadService.getValidationErrors(response, fileId);
	}

	@GetMapping("/validationErrorsBySubmission")
	public void exportValidationErrorsBySubmissionToCSV(HttpServletResponse response,
														@AuthenticationPrincipal Jwt jwt,
														@RequestParam("submissionId") Integer submissionId) {
		authenticationService.checkAuth(jwt, List.of(AccessRole.DATA_CURATOR, AccessRole.DATA_SUBMITTER));
		//TODO: limit access to submitters
		downloadService.getValidationErrorsbySubmission(response, submissionId);
	}
}
