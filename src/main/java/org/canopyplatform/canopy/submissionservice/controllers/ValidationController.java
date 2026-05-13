package org.canopyplatform.canopy.submissionservice.controllers;

import org.canopyplatform.canopy.submissionservice.auth.core.KeycloakAuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import org.canopyplatform.canopy.submissionservice.exceptions.custom.ValidationErrorException;
import org.canopyplatform.canopy.submissionservice.models.dtos.ValidationResultsDTO;
import org.canopyplatform.canopy.submissionservice.services.BundleService;
import org.canopyplatform.canopy.submissionservice.services.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequestMapping("/validateFiles")
@RestController
@RequiredArgsConstructor
public class ValidationController {

	private final ValidationService validationService;
	private final BundleService bundleService;
  private final KeycloakAuthenticationService authenticationService;

	@PostMapping("/validate")
	public ResponseEntity<String> cdeValidation(@AuthenticationPrincipal Jwt jwt,
												@RequestParam("submissionId") Integer submissionId) {
		authenticationService.checkCapability(jwt, "submission.validate");
		//TODO: submission authorization check
		//TODO: clean up error handling
		try {
			boolean filesValidated = validationService.validateFiles(submissionId);

			if (filesValidated) {
				// update submissionInfo validate to true
				validationService.updateSubmissionIsValidated(submissionId, true);
				return ResponseEntity.ok("Validation completed successfully");
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Unable to successfully perform validation. An error occurred while validating files");
			}
		} catch (ValidationErrorException | IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			log.error("Error validating files", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while validating files");
		}
	}

	@GetMapping("/getResults")
	public ValidationResultsDTO getValidationResults(@AuthenticationPrincipal Jwt jwt,
													 @RequestParam("submissionId") Integer submissionId) {
		authenticationService.checkCapability(jwt, "submission.validation.read");
		return validationService.getSubmissionValidationResults(submissionId);
	}

	@PostMapping("/acknowledge")
	public ResponseEntity<Boolean> updateFileAcknowledgements(@AuthenticationPrincipal Jwt jwt,
															  @RequestBody ValidationResultsDTO dto,
															  @RequestParam("submit") Boolean shouldSubmit) {
		Integer userId = authenticationService.checkCapability(jwt, "submission.validation.acknowledge");
		//TODO: submission authorization check
		Boolean allFilesWithWarningsAcknowledged = validationService.updateFileAck(dto, userId);
		boolean shouldIncrementStep = allFilesWithWarningsAcknowledged && shouldSubmit;
		if (shouldIncrementStep) {
			String stepDescription = "Validate Files";
			bundleService.updateStepId(dto.getSubmissionId(), stepDescription, userId);
		}
		return ResponseEntity.ok(shouldIncrementStep);
	}

}
