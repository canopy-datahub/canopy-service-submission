package ex.org.project.submissionService.controllers;

import java.util.List;

import ex.org.project.submissionService.auth.AccessRole;
import ex.org.project.submissionService.auth.UserAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ex.org.project.submissionService.models.dtos.SubmissionInfoDTO;
import ex.org.project.submissionService.services.SubmitterService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class SubmitterDashboardController {

	private final SubmitterService submitterService;
	private final UserAuthService authService;

	@GetMapping("/getSubmissions")
	public ResponseEntity<List<SubmissionInfoDTO>> getSubmissions(@CookieValue(value="chocolateChip", required = false) String sessionId,
																  @RequestParam("status") String status) {
		Integer userId = authService.checkAuth(sessionId, List.of(AccessRole.DATA_SUBMITTER));
		//TODO: submission authorization
		List<SubmissionInfoDTO> userSubmissions = submitterService.getSubmissions(userId, status);
		return ResponseEntity.ok(userSubmissions);
	}

	@DeleteMapping("/deleteSubmission")
	public ResponseEntity<String> deleteSubmission(@CookieValue(value="chocolateChip", required = false) String sessionId,
												   @RequestParam("submissionId") Integer submissionId) {
		authService.checkAuth(sessionId, List.of(AccessRole.DATA_SUBMITTER));
		//TODO: submission authorization
		submitterService.deleteSubmission(submissionId);
		return new ResponseEntity<>("Submission successfully deleted", HttpStatus.OK);
	}

}
