package org.canopyplatform.canopy.submissionservice.controllers;

import java.util.List;
import java.util.NoSuchElementException;

import org.canopyplatform.canopy.submissionservice.auth.core.KeycloakAuthenticationService;
import org.canopyplatform.canopy.submissionservice.models.dtos.DetailsDTO;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import org.canopyplatform.canopy.submissionservice.models.dtos.DataSubmissionDTO;
import org.canopyplatform.canopy.submissionservice.models.dtos.SubmissionApprovalDTO;
import org.canopyplatform.canopy.submissionservice.services.FileApprovalService;
import lombok.RequiredArgsConstructor;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/curator")
public class CuratorController {

	private final FileApprovalService approvalService;
  private final KeycloakAuthenticationService authenticationService;

  @GetMapping("/getSubmissions")
	public ResponseEntity<?> getSubmittedSubmissions(@AuthenticationPrincipal Jwt jwt,
													 @RequestParam("status") String status) {
		authenticationService.checkCapability(jwt, "curator.submission.list");
		//TODO: refactor errors
		List<DataSubmissionDTO> submissions = approvalService.getSubmittedSubmissions(status);
		return ResponseEntity.ok(submissions);
	}

	@GetMapping("/getFilesBySubm")
	public ResponseEntity<DetailsDTO> getFiles(@AuthenticationPrincipal Jwt jwt,
											   @RequestParam("submissionId") Integer submissionId) {
		System.out.println("Raw submissionId parameter: " + submissionId);
		authenticationService.checkCapability(jwt, "curator.submission.read");
		return ResponseEntity.ok(approvalService.getSubmissionBundleInfo(submissionId));
	}

	@PostMapping("/processFiles")
	public ResponseEntity<String> processFiles(@AuthenticationPrincipal Jwt jwt,
											   @RequestBody SubmissionApprovalDTO dto) {
		authenticationService.checkCapability(jwt, "curator.submission.review");
		//TODO: refactor errors
		try {
			approvalService.processSubmission(dto);
			return ResponseEntity.ok().build();
		} catch (NoSuchElementException e) {
			log.error("no such element exception", e);
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}


	@GetMapping("/all-submission-files")
	public ResponseEntity<Object> downloadAllSubmissionFiles(@AuthenticationPrincipal Jwt jwt,
															 @RequestParam Integer submissionId){
		authenticationService.checkCapability(jwt, "curator.submission.download.bulk");
		return approvalService.getAllSubmissionFiles(submissionId);
	}
}
