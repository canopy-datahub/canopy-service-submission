package ex.org.project.submissionService.controllers;

import ex.org.project.submissionService.auth.AccessRole;
import ex.org.project.submissionService.auth.UserAuthService;
import ex.org.project.submissionService.models.dtos.StudiesDTO;
import ex.org.project.submissionService.models.dtos.UploadPortalCuratorDashboardDTO;
import ex.org.project.submissionService.services.UploadPortalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/uploadPortal")
public class UploadPortalController {

    private final UserAuthService authService;
    private final UploadPortalService uploadPortalService;

    @PostMapping("/upload/{studyId}")
    public void uploadFile(@CookieValue(value="chocolateChip", required = false) String sessionId,
                           @RequestParam("file") MultipartFile file, @PathVariable Integer studyId) {
        Integer userId = authService.checkAuth(sessionId, List.of(AccessRole.UPLOADER));
        uploadPortalService.uploadFile(file, studyId, userId);
    }

    @GetMapping("/getStudies")
    public ResponseEntity<List<StudiesDTO>> getApprovedStudies(@CookieValue(value="chocolateChip", required = false) String sessionId) {
        authService.checkAuth(sessionId, List.of(AccessRole.UPLOADER));
        return ResponseEntity.ok(uploadPortalService.getApprovedStudies());
    }

    @GetMapping("/curator/dashboard")
    public ResponseEntity<List<UploadPortalCuratorDashboardDTO>> getCuratorDashboardView(@CookieValue(value="chocolateChip", required = false) String sessionId){
        authService.checkAuth(sessionId, List.of(AccessRole.DATA_CURATOR));
        return ResponseEntity.ok(uploadPortalService.getCuratorDashboardView());
    }

    @DeleteMapping("/curator/dashboard/delete")
    public ResponseEntity<Void> deleteUpload(@CookieValue(value="chocolateChip", required = false) String sessionId,
                                           @RequestParam("uploadId") Integer uploadId){
        Integer userId = authService.checkAuth(sessionId, List.of(AccessRole.DATA_CURATOR));
        uploadPortalService.deleteUpload(uploadId, userId);
        return ResponseEntity.ok().build();
    }
}
