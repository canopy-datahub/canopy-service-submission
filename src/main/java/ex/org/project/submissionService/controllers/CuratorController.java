package ex.org.project.submissionService.controllers;

import java.util.List;
import java.util.NoSuchElementException;

import ex.org.project.submissionService.auth.AccessRole;
import ex.org.project.submissionService.auth.core.KeycloakAuthenticationService;
import ex.org.project.submissionService.models.dtos.DetailsDTO;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import ex.org.project.submissionService.models.dtos.DataSubmissionDTO;
import ex.org.project.submissionService.models.dtos.SubmissionApprovalDTO;
import ex.org.project.submissionService.services.FileApprovalService;
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
		authenticationService.checkAuth(jwt, List.of(AccessRole.DATA_CURATOR));
		//TODO: refactor errors
		List<DataSubmissionDTO> submissions = approvalService.getSubmittedSubmissions(status);
		return ResponseEntity.ok(submissions);
	}

	@GetMapping("/getFilesBySubm")
	public ResponseEntity<DetailsDTO> getFiles(@AuthenticationPrincipal Jwt jwt,
											   @RequestParam("submissionId") Integer submissionId) {
		System.out.println("Raw submissionId parameter: " + submissionId);
		authenticationService.checkAuth(jwt, List.of(AccessRole.DATA_CURATOR));
		return ResponseEntity.ok(approvalService.getSubmissionBundleInfo(submissionId));
	}

	@PostMapping("/processFiles")
	public ResponseEntity<String> processFiles(@AuthenticationPrincipal Jwt jwt,
											   @RequestBody SubmissionApprovalDTO dto) {
		authenticationService.checkAuth(jwt, List.of(AccessRole.DATA_CURATOR));
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
		authenticationService.checkAuth(jwt, List.of(AccessRole.DATA_CURATOR));
		return approvalService.getAllSubmissionFiles(submissionId);
	}
}
