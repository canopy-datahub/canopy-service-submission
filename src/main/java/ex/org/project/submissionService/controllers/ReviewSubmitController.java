package ex.org.project.submissionService.controllers;

import java.util.List;
import java.util.NoSuchElementException;

import ex.org.project.submissionService.auth.AccessRole;
import ex.org.project.submissionService.auth.UserAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ex.org.project.submissionService.services.BundleService;
import ex.org.project.submissionService.services.SubmissionService;
import lombok.RequiredArgsConstructor;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/reviewAndSubmit")
public class ReviewSubmitController {

	private final SubmissionService submissionService;
	private final BundleService bundleService;
	private final UserAuthService authService;

	@PostMapping("/submit")
	public ResponseEntity<String> submit(@CookieValue(value="chocolateChip", required = false) String sessionId,
										 @RequestParam("submissionId") Integer submissionId) {
		Integer userId = authService.checkAuth(sessionId, List.of(AccessRole.DATA_SUBMITTER));
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
