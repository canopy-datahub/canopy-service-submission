package org.canopyplatform.canopy.submissionservice.controllers;

import java.util.List;

import org.canopyplatform.canopy.submissionservice.auth.core.KeycloakAuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import org.canopyplatform.canopy.submissionservice.models.dtos.SubmissionInfoDTO;
import org.canopyplatform.canopy.submissionservice.services.SubmitterService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class SubmitterDashboardController {

	private final SubmitterService submitterService;
  private final KeycloakAuthenticationService authenticationService;

	@GetMapping("/getSubmissions")
	public ResponseEntity<List<SubmissionInfoDTO>> getSubmissions(@AuthenticationPrincipal Jwt jwt,
																  @RequestParam("status") String status) {
		Integer userId = authenticationService.checkCapability(jwt, "submission.read.own");
		//TODO: submission authorization
		List<SubmissionInfoDTO> userSubmissions = submitterService.getSubmissions(userId, status);
		return ResponseEntity.ok(userSubmissions);
	}

	@DeleteMapping("/deleteSubmission")
	public ResponseEntity<String> deleteSubmission(@AuthenticationPrincipal Jwt jwt,
												   @RequestParam("submissionId") Integer submissionId) {
		authenticationService.checkCapability(jwt, "submission.delete.own");
		//TODO: submission authorization
		submitterService.deleteSubmission(submissionId);
		return new ResponseEntity<>("Submission successfully deleted", HttpStatus.OK);
	}

}
