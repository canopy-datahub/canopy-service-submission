package org.canopyplatform.canopy.submissionservice.controllers;

import java.util.List;
import java.util.NoSuchElementException;

import org.canopyplatform.canopy.submissionservice.auth.core.KeycloakAuthenticationService;
import org.canopyplatform.canopy.submissionservice.services.StudyAccessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import org.canopyplatform.canopy.submissionservice.services.BundleService;
import org.canopyplatform.canopy.submissionservice.services.SubmissionService;
import lombok.RequiredArgsConstructor;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/reviewAndSubmit")
public class ReviewSubmitController {

	private final SubmissionService submissionService;
	private final BundleService bundleService;
  private final KeycloakAuthenticationService authenticationService;
  private final StudyAccessService studyAccessService;

	@PostMapping("/submit")
	public ResponseEntity<String> submit(@AuthenticationPrincipal Jwt jwt,
										 @RequestParam("submissionId") Integer submissionId) {
		Integer userId = authenticationService.checkCapability(jwt, "submission.submit");
		studyAccessService.requireSubmitToBySubmission(jwt, submissionId);
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
