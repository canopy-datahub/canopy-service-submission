package ex.org.project.submissionService.controllers;

import ex.org.project.submissionService.auth.AccessRole;
import ex.org.project.submissionService.auth.UserAuthService;
import ex.org.project.submissionService.services.DownloadService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/download")
public class DownloadController {

	private final DownloadService downloadService;
	private final UserAuthService authService;

	@GetMapping("/validationErrorsByFile")
	public void exportValidationErrorsByFileIdToCSV(HttpServletResponse response,
													@CookieValue(value="chocolateChip", required = false) String sessionId,
													@RequestParam("fileId") Integer fileId) {
		//TODO: file authorization
		authService.checkAuth(sessionId, List.of(AccessRole.DATA_CURATOR, AccessRole.DATA_SUBMITTER));
		downloadService.getValidationErrors(response, fileId);
	}

	@GetMapping("/validationErrorsBySubmission")
	public void exportValidationErrorsBySubmissionToCSV(HttpServletResponse response,
														@CookieValue(value="chocolateChip", required = false) String sessionId,
														@RequestParam("submissionId") Integer submissionId) {
		authService.checkAuth(sessionId, List.of(AccessRole.DATA_CURATOR, AccessRole.DATA_SUBMITTER));
		//TODO: limit access to submitters
		downloadService.getValidationErrorsbySubmission(response, submissionId);
	}
}
