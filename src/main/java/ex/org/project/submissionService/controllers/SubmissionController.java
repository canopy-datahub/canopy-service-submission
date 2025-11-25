package ex.org.project.submissionService.controllers;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import ex.org.project.submissionService.auth.AccessRole;
import ex.org.project.submissionService.auth.UserAuthService;
import ex.org.project.submissionService.exceptions.custom.DataFileNotFoundException;
import ex.org.project.submissionService.exceptions.custom.FileDeletionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ex.org.project.submissionService.models.DataFileCategory;
import ex.org.project.submissionService.models.dtos.StudiesDTO;
import ex.org.project.submissionService.models.dtos.SubmissionStepDTO;
import ex.org.project.submissionService.models.dtos.ValidationResultsDTO;
import ex.org.project.submissionService.services.BundleService;
import ex.org.project.submissionService.services.DataFileService;
import ex.org.project.submissionService.services.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
@RequiredArgsConstructor
public class SubmissionController {

	private final SubmissionService studyService;
	private final DataFileService datafileService;
	private final BundleService bundleService;
	private final UserAuthService authService;

	@GetMapping("/getStudies")
	public ResponseEntity<List<StudiesDTO>> getStudiesByUserCenter(@CookieValue(value="chocolateChip", required = false) String sessionId) {
		Integer userId = authService.checkAuth(sessionId, List.of(AccessRole.DATA_SUBMITTER));
		return ResponseEntity.ok(studyService.getStudiesByUserCenter(userId));
	}

	@PostMapping("/create-submission")
	public ResponseEntity<?> createSubmission(@CookieValue(value="chocolateChip", required = false) String sessionId,
											  @RequestParam("studyId") Integer studyId) {
		Integer userId = authService.checkAuth(sessionId, List.of(AccessRole.DATA_SUBMITTER));
		//TODO: clean up error handling
		try {
			Integer submissionId = studyService.createSubmission(studyId, userId);
			return ResponseEntity.status(HttpStatus.CREATED).body(submissionId);
		} catch (Exception e) {
			log.error("Unknown Exception", e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}
	}

	@GetMapping("/getCategories")
	public Map<String, List<DataFileCategory>> getDataFileCategories(@CookieValue(value="chocolateChip", required = false) String sessionId) {
		authService.checkAuth(sessionId, List.of(AccessRole.DATA_SUBMITTER));
		return bundleService.getDataFileCategories();
	}

	@GetMapping("/submissionInfo")
	public ResponseEntity<?> submissionInfo(@CookieValue(value="chocolateChip", required = false) String sessionId,
											@RequestParam("submissionId") Integer submissionId) {
		authService.checkAuth(sessionId, List.of(AccessRole.DATA_SUBMITTER));
		//TODO: submission authorization
		//TODO: clean up error handling
		try {
			SubmissionStepDTO submissioninfo = bundleService.getSubmissionInfo(submissionId);
			return ResponseEntity.ok(submissioninfo);
		} catch (NoSuchElementException e) {
			log.error("no such element exception", e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		}
	}

	@DeleteMapping("/deleteFiles")
	public ResponseEntity<String> deleteFile(@CookieValue(value="chocolateChip", required = false) String sessionId,
											 @RequestParam("fileIds") List<Integer> fileIds) {
		authService.checkAuth(sessionId, List.of(AccessRole.DATA_SUBMITTER));
		try{
			boolean allDeleted = datafileService.deleteMultipleDatafiles(fileIds);
			return ResponseEntity.status(HttpStatus.OK).body("Files were deleted: " + allDeleted);
		}catch(DataFileNotFoundException | FileDeletionException e){
			log.error("Error deleting file", e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		}
	}

	@PutMapping("/replaceFile")
	public ResponseEntity<ValidationResultsDTO> replaceFile(@CookieValue(value="chocolateChip", required = false) String sessionId,
															@RequestParam("fileId") Integer fileId,
															@RequestParam("newFile") MultipartFile file) {
		Integer userId = authService.checkAuth(sessionId, List.of(AccessRole.DATA_SUBMITTER));
		//TODO: file authorization, maybe add status handling
		ValidationResultsDTO results = datafileService.replaceDataFile(fileId, file, userId);
		return new ResponseEntity<>(results, HttpStatus.OK);
	}

}
