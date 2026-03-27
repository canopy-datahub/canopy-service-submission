package ex.org.project.submissionService.controllers;

import ex.org.project.submissionService.auth.AccessRole;
import ex.org.project.submissionService.auth.core.KeycloakAuthenticationService;
import ex.org.project.submissionService.models.dtos.StudiesDTO;
import ex.org.project.submissionService.models.dtos.UploadPortalCuratorDashboardDTO;
import ex.org.project.submissionService.services.UploadPortalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/uploadPortal")
public class UploadPortalController {

    private final KeycloakAuthenticationService authenticationService;
    private final UploadPortalService uploadPortalService;

    @PostMapping("/upload/{studyId}")
    public void uploadFile(@AuthenticationPrincipal Jwt jwt,
                           @RequestParam("file") MultipartFile file, @PathVariable Integer studyId) {
        Integer userId = authenticationService.checkAuth(jwt, List.of(AccessRole.UPLOADER));
        uploadPortalService.uploadFile(file, studyId, userId);
    }

    @GetMapping("/getStudies")
    public ResponseEntity<List<StudiesDTO>> getApprovedStudies(@AuthenticationPrincipal Jwt jwt) {
        authenticationService.checkAuth(jwt, List.of(AccessRole.UPLOADER));
        return ResponseEntity.ok(uploadPortalService.getApprovedStudies());
    }

    @GetMapping("/curator/dashboard")
    public ResponseEntity<List<UploadPortalCuratorDashboardDTO>> getCuratorDashboardView(@AuthenticationPrincipal Jwt jwt){
        authenticationService.checkAuth(jwt, List.of(AccessRole.DATA_CURATOR));
        return ResponseEntity.ok(uploadPortalService.getCuratorDashboardView());
    }

    @DeleteMapping("/curator/dashboard/delete")
    public ResponseEntity<Void> deleteUpload(@AuthenticationPrincipal Jwt jwt,
                                           @RequestParam("uploadId") Integer uploadId){
        Integer userId = authenticationService.checkAuth(jwt, List.of(AccessRole.DATA_CURATOR));
        uploadPortalService.deleteUpload(uploadId, userId);
        return ResponseEntity.ok().build();
    }
}
