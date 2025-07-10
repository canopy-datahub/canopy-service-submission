package ex.org.project.submissionService.controllers;

import java.util.List;
import java.util.NoSuchElementException;

import ex.org.project.submissionService.auth.AccessRole;
import ex.org.project.submissionService.auth.UserAuthService;
import ex.org.project.submissionService.models.dtos.DetailsDTO;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
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
	private final UserAuthService authService;

	@GetMapping("/getSubmissions")
	public ResponseEntity<?> getSubmittedSubmissions(@CookieValue(value="chocolateChip", required = false) String sessionId,
													 @RequestParam("status") String status) {
		authService.checkAuth(sessionId, List.of(AccessRole.DATA_CURATOR));
		//TODO: refactor errors
		List<DataSubmissionDTO> submissions = approvalService.getSubmittedSubmissions(status);
		return ResponseEntity.ok(submissions);
	}

	@GetMapping("/getFilesBySubm")
	public ResponseEntity<DetailsDTO> getFiles(@CookieValue(value="chocolateChip", required = false) String sessionId,
											   @RequestParam("submissionId") Integer submissionId) {
		authService.checkAuth(sessionId, List.of(AccessRole.DATA_CURATOR));
		return ResponseEntity.ok(approvalService.getSubmissionBundleInfo(submissionId));
	}

	@PostMapping("/processFiles")
	public ResponseEntity<String> processFiles(@CookieValue(value="chocolateChip", required = false) String sessionId,
											   @RequestBody SubmissionApprovalDTO dto) {
		authService.checkAuth(sessionId, List.of(AccessRole.DATA_CURATOR));
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
	public ResponseEntity<Object> downloadAllSubmissionFiles(@CookieValue(value="chocolateChip", required = false) String sessionId,
															 @RequestParam Integer submissionId){
		authService.checkAuth(sessionId, List.of(AccessRole.DATA_CURATOR));
		return approvalService.getAllSubmissionFiles(submissionId);
	}
}
