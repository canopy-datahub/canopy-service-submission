package ex.org.project.submissionService.controllers;

import ex.org.project.submissionService.auth.AccessRole;
import ex.org.project.submissionService.auth.UserAuthService;
import ex.org.project.submissionService.exceptions.custom.BadDataException;
import ex.org.project.submissionService.models.dtos.GetBundleFilesDTO;
import ex.org.project.submissionService.models.dtos.SubmissionBundlesDTO;
import ex.org.project.submissionService.services.BundleService;
import ex.org.project.submissionService.services.DataFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bundle")
public class BundleController {

    private final BundleService bundleService;
    private final DataFileService dataFileService;
    private final UserAuthService authService;

    @GetMapping("/get")
    public SubmissionBundlesDTO getBundles(@CookieValue(value="chocolateChip", required = false) String sessionId,
                                           @RequestParam("submissionId") Integer submissionId) {
        authService.checkAuth(sessionId, List.of(AccessRole.DATA_SUBMITTER));
        //TODO: submission authorization check
        return bundleService.getBundles(submissionId);
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateBundles(@CookieValue(value="chocolateChip", required = false) String sessionId,
                                                @RequestBody SubmissionBundlesDTO dto) {
        Integer userId = authService.checkAuth(sessionId, List.of(AccessRole.DATA_SUBMITTER));
        //TODO: bundle authorization check
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
    public ResponseEntity<String> deleteBundle(@CookieValue(value="chocolateChip", required = false) String sessionId,
                                               @RequestParam("fileId") Integer fileId) {
        authService.checkAuth(sessionId, List.of(AccessRole.DATA_SUBMITTER));
        //TODO: bundle authorization check
        List<Integer> successfulFileIds = dataFileService.deleteBundle(fileId);
        return new ResponseEntity<>("Files deleted: " + successfulFileIds, HttpStatus.OK);
    }

    @GetMapping("/getFiles")
    public ResponseEntity<GetBundleFilesDTO> getBundleFiles(@CookieValue(value="chocolateChip", required = false) String sessionId,
                                            @RequestParam("fileId") Integer fileId){
        authService.checkAuth(sessionId, List.of(AccessRole.DATA_SUBMITTER));
        //TODO: bundle authorization check
        GetBundleFilesDTO files = bundleService.getBundleFiles(fileId);
        return ResponseEntity.ok().body(files);
    }

    @PostMapping("/previousPage")
    public ResponseEntity<String> goBackAndUploadFiles(@CookieValue(value="chocolateChip", required = false) String sessionId,
                                                       @RequestParam("submissionId")Integer submissionId){
        authService.checkAuth(sessionId, List.of(AccessRole.DATA_SUBMITTER));
        bundleService.goBackAndUploadFiles(submissionId);
        return ResponseEntity.ok().build();
    }


}

