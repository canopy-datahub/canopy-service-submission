package org.canopyplatform.canopy.submissionservice.controllers;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.canopyplatform.canopy.submissionservice.auth.core.KeycloakAuthenticationService;
import org.canopyplatform.canopy.submissionservice.exceptions.custom.DataFileNotFoundException;
import org.canopyplatform.canopy.submissionservice.exceptions.custom.FileDeletionException;
import org.canopyplatform.canopy.submissionservice.services.StudyAccessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import org.canopyplatform.canopy.submissionservice.models.DataFileCategory;
import org.canopyplatform.canopy.submissionservice.models.dtos.StudiesDTO;
import org.canopyplatform.canopy.submissionservice.models.dtos.SubmissionStepDTO;
import org.canopyplatform.canopy.submissionservice.models.dtos.ValidationResultsDTO;
import org.canopyplatform.canopy.submissionservice.services.BundleService;
import org.canopyplatform.canopy.submissionservice.services.DataFileService;
import org.canopyplatform.canopy.submissionservice.services.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
@RequiredArgsConstructor
public class SubmissionController {

	private final SubmissionService studyService;
	private final DataFileService datafileService;
	private final BundleService bundleService;
  private final KeycloakAuthenticationService authenticationService;
  private final StudyAccessService studyAccessService;

	@GetMapping("/getStudies")
	public ResponseEntity<List<StudiesDTO>> getStudiesByUserCenter(@AuthenticationPrincipal Jwt jwt) {
		Integer userId = authenticationService.checkCapability(jwt, "submission.config.read");
		return ResponseEntity.ok(studyService.getStudiesByUserCenter(userId));
	}

	@PostMapping("/create-submission")
	public ResponseEntity<?> createSubmission(@AuthenticationPrincipal Jwt jwt,
											  @RequestParam("studyId") Integer studyId) {
		Integer userId = authenticationService.checkCapability(jwt, "submission.create");
		studyAccessService.requireSubmitTo(jwt, studyId);
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
	public Map<String, List<DataFileCategory>> getDataFileCategories(@AuthenticationPrincipal Jwt jwt) {
		authenticationService.checkCapability(jwt, "submission.config.read");
		return bundleService.getDataFileCategories();
	}

	@GetMapping("/submissionInfo")
	public ResponseEntity<?> submissionInfo(@AuthenticationPrincipal Jwt jwt,
											@RequestParam("submissionId") Integer submissionId) {
		authenticationService.checkCapability(jwt, "submission.read.own");
		studyAccessService.requireStudyManagementBySubmission(jwt, submissionId);
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
	public ResponseEntity<String> deleteFile(@AuthenticationPrincipal Jwt jwt,
											 @RequestParam("fileIds") List<Integer> fileIds) {
		authenticationService.checkCapability(jwt, "submission.file.delete");
		// Strict creator-only — files belong to a submission, which belongs to a study.
		for (Integer fileId : fileIds) {
			studyAccessService.requireSubmitToByFile(jwt, fileId);
		}
		try{
			boolean allDeleted = datafileService.deleteMultipleDatafiles(fileIds);
			return ResponseEntity.status(HttpStatus.OK).body("Files were deleted: " + allDeleted);
		}catch(DataFileNotFoundException | FileDeletionException e){
			log.error("Error deleting file", e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		}
	}

	@PutMapping("/replaceFile")
	public ResponseEntity<ValidationResultsDTO> replaceFile(@AuthenticationPrincipal Jwt jwt,
															@RequestParam("fileId") Integer fileId,
															@RequestParam("newFile") MultipartFile file) {
		Integer userId = authenticationService.checkCapability(jwt, "submission.file.replace");
		studyAccessService.requireSubmitToByFile(jwt, fileId);
		ValidationResultsDTO results = datafileService.replaceDataFile(fileId, file, userId);
		return new ResponseEntity<>(results, HttpStatus.OK);
	}

}
