package org.canopyplatform.canopy.submissionservice.controllers;

import org.canopyplatform.canopy.submissionservice.auth.core.KeycloakAuthenticationService;
import org.canopyplatform.canopy.submissionservice.exceptions.custom.BadDataException;
import org.canopyplatform.canopy.submissionservice.models.dtos.GetBundleFilesDTO;
import org.canopyplatform.canopy.submissionservice.models.dtos.SubmissionBundlesDTO;
import org.canopyplatform.canopy.submissionservice.services.BundleService;
import org.canopyplatform.canopy.submissionservice.services.DataFileService;
import org.canopyplatform.canopy.submissionservice.services.StudyAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bundle")
public class BundleController {

    private final BundleService bundleService;
    private final DataFileService dataFileService;
    private final KeycloakAuthenticationService authenticationService;
    private final StudyAccessService studyAccessService;

    @GetMapping("/get")
    public SubmissionBundlesDTO getBundles(@AuthenticationPrincipal Jwt jwt,
                                           @RequestParam("submissionId") Integer submissionId) {
        authenticationService.checkCapability(jwt, "submission.bundle.read");
        studyAccessService.requireStudyManagementBySubmission(jwt, submissionId);
        return bundleService.getBundles(submissionId);
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateBundles(@AuthenticationPrincipal Jwt jwt,
                                                @RequestBody SubmissionBundlesDTO dto) {
        Integer userId = authenticationService.checkCapability(jwt, "submission.bundle.update");
        studyAccessService.requireSubmitToBySubmission(jwt, dto.getSubmissionId());
        try {
            bundleService.updateBundles(dto);
            String stepdescription="Bundle Files";
            bundleService.updateStepId(dto.getSubmissionId(),stepdescription, userId);
            return ResponseEntity.ok().build();

        } catch (BadDataException | DataAccessException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteBundle(@AuthenticationPrincipal Jwt jwt,
                                               @RequestParam("fileId") Integer fileId) {
        authenticationService.checkCapability(jwt, "submission.bundle.delete");
        studyAccessService.requireSubmitToByFile(jwt, fileId);
        List<Integer> successfulFileIds = dataFileService.deleteBundle(fileId);
        return new ResponseEntity<>("Files deleted: " + successfulFileIds, HttpStatus.OK);
    }

    @GetMapping("/getFiles")
    public ResponseEntity<GetBundleFilesDTO> getBundleFiles(@AuthenticationPrincipal Jwt jwt,
                                            @RequestParam("fileId") Integer fileId){
        authenticationService.checkCapability(jwt, "submission.bundle.read");
        studyAccessService.requireSubmitToByFile(jwt, fileId);
        GetBundleFilesDTO files = bundleService.getBundleFiles(fileId);
        return ResponseEntity.ok().body(files);
    }

    @PostMapping("/previousPage")
    public ResponseEntity<String> goBackAndUploadFiles(@AuthenticationPrincipal Jwt jwt,
                                                       @RequestParam("submissionId")Integer submissionId){
        authenticationService.checkCapability(jwt, "submission.bundle.previousPage");
        studyAccessService.requireSubmitToBySubmission(jwt, submissionId);
        bundleService.goBackAndUploadFiles(submissionId);
        return ResponseEntity.ok().build();
    }


}

