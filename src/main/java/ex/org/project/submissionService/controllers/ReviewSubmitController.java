package ex.org.project.submissionService.controllers;

import ex.org.project.datahub.auth.core.KeycloakAuthenticationService;
import ex.org.project.datahub.auth.model.AccessRole;
import ex.org.project.submissionService.services.BundleService;
import ex.org.project.submissionService.services.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/reviewAndSubmit")
public class ReviewSubmitController {

	private final SubmissionService submissionService;
	private final BundleService bundleService;
	private final KeycloakAuthenticationService authenticationService;

	@PostMapping("/submit")
	public ResponseEntity<String> submit(@AuthenticationPrincipal Jwt jwt,
										 @RequestParam("submissionId") Integer submissionId) {
		Integer userId = authenticationService.checkAuth(jwt, List.of(AccessRole.DATA_SUBMITTER));
		//TODO: submission authorization check
		//TODO: refactor errors
		try {
			submissionService.submit(submissionId);
			String stepDescription = "Submitted";
			bundleService.updateStepId(submissionId, stepDescription, userId);
			return ResponseEntity.ok().build();
		} catch (NoSuchElementException e) {
			log.error("no such element exception", e);
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

}
