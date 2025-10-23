package ex.org.project.submissionService.controllers;

import ex.org.project.datahub.auth.core.KeycloakAuthenticationService;
import ex.org.project.datahub.auth.model.AccessRole;
import ex.org.project.submissionService.models.dtos.SubmissionInfoDTO;
import ex.org.project.submissionService.services.SubmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@RestController
@RequiredArgsConstructor
public class SubmitterDashboardController {

	private final SubmitterService submitterService;
	private final KeycloakAuthenticationService authenticationService;

	@GetMapping("/getSubmissions")
	public ResponseEntity<List<SubmissionInfoDTO>> getSubmissions(@AuthenticationPrincipal Jwt jwt,
																  @RequestParam("status") String status) {
		Integer userId = authenticationService.checkAuth(jwt, List.of(AccessRole.DATA_SUBMITTER));
		//TODO: submission authorization
		List<SubmissionInfoDTO> userSubmissions = submitterService.getSubmissions(userId, status);
		return ResponseEntity.ok(userSubmissions);
	}

	@DeleteMapping("/deleteSubmission")
	public ResponseEntity<String> deleteSubmission(@AuthenticationPrincipal Jwt jwt,
												   @RequestParam("submissionId") Integer submissionId) {
		authenticationService.checkAuth(jwt, List.of(AccessRole.DATA_SUBMITTER));
		//TODO: submission authorization
		submitterService.deleteSubmission(submissionId);
		return new ResponseEntity<>("Submission successfully deleted", HttpStatus.OK);
	}

}
