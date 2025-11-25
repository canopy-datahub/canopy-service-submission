package ex.org.project.submissionService.controllers;

import ex.org.project.datahub.auth.core.KeycloakAuthenticationService;
import ex.org.project.datahub.auth.model.AccessRole;
import ex.org.project.submissionService.exceptions.custom.BadDataException;
import ex.org.project.submissionService.models.dtos.S3FileDTO;
import ex.org.project.submissionService.models.dtos.UploadFilesDTO;
import ex.org.project.submissionService.services.BundleService;
import ex.org.project.submissionService.services.DataFileService;
import ex.org.project.submissionService.services.SFTPService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.NoSuchElementException;
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/uploadFiles")
public class UploadController {

	private final BundleService bundleService;
	private final DataFileService datafileService;
	private final SFTPService sftpService;
	private final KeycloakAuthenticationService authenticationService;


	@GetMapping("/getFiles")
	public ResponseEntity<?> getFiles(@AuthenticationPrincipal Jwt jwt,
									  @RequestParam("submissionId") Integer submissionId) {
		authenticationService.checkAuth(jwt, List.of(AccessRole.DATA_SUBMITTER));
		//TODO: clean up error handling
		try {
			UploadFilesDTO uploadedFiles = datafileService.getUploadedFiles(submissionId);
			return ResponseEntity.ok(uploadedFiles);
		} catch (NoSuchElementException e) {
			log.error("no such element exception", e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		}
	}

	@PostMapping("/multiple")
	public ResponseEntity<List<S3FileDTO>> uploadFiles(@AuthenticationPrincipal Jwt jwt,
													   @RequestParam("files") List<MultipartFile> files,
													   @RequestParam("submissionId") Integer submissionId) {
		Integer userId = authenticationService.checkAuth(jwt, List.of(AccessRole.DATA_SUBMITTER));
		List<S3FileDTO> s3FileDTOS = datafileService.createDataFiles(files, submissionId, userId);
		return ResponseEntity.ok().body(s3FileDTOS);
	}

	@PostMapping("/createBundles")
	public ResponseEntity<String> createBundles(@AuthenticationPrincipal Jwt jwt,
												@RequestParam("submissionId") Integer submissionId) {
		Integer userId = authenticationService.checkAuth(jwt, List.of(AccessRole.DATA_SUBMITTER));
		//TODO: clean up error handling
			boolean bundlesCreated = bundleService.createBundles(submissionId);
			String stepDescription = "Upload Files";
			bundleService.updateStepId(submissionId, stepDescription, userId);
			if (bundlesCreated) {
				return ResponseEntity.ok().build();
			}else{
				log.error("Bundling failed for submission ID " + submissionId);
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Bundling failed for submission ID " + submissionId);
			}
	}

	@SqsListener(value = "${SFTPQueue}")
	@GetMapping("/processSFTP")
	public ResponseEntity<String> processSFTP(Message message) {
		try{
			boolean sftpProcessed = sftpService.processSFTPUpload(message.body(), message.receiptHandle());
			if(sftpProcessed){
				return ResponseEntity.ok().build();
			}else {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
			}
		}
		catch (BadDataException e){
			log.error("BadDataException Exception", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}
	}
}
