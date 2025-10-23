package ex.org.project.submissionService.controllers;

import ex.org.project.datahub.auth.core.KeycloakAuthenticationService;
import ex.org.project.datahub.auth.model.AccessRole;
import ex.org.project.submissionService.exceptions.custom.ValidationErrorException;
import ex.org.project.submissionService.models.dtos.ValidationResultsDTO;
import ex.org.project.submissionService.services.BundleService;
import ex.org.project.submissionService.services.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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
		authenticationService.checkAuth(jwt, List.of(AccessRole.DATA_SUBMITTER));
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
		authenticationService.checkAuth(jwt, List.of(AccessRole.DATA_SUBMITTER, AccessRole.DATA_CURATOR));
		return validationService.getSubmissionValidationResults(submissionId);
	}

	@PostMapping("/acknowledge")
	public ResponseEntity<Boolean> updateFileAcknowledgements(@AuthenticationPrincipal Jwt jwt,
															  @RequestBody ValidationResultsDTO dto,
															  @RequestParam("submit") Boolean shouldSubmit) {
		Integer userId = authenticationService.checkAuth(jwt, List.of(AccessRole.DATA_SUBMITTER));
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
