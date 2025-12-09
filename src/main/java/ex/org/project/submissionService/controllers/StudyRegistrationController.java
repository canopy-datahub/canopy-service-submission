package ex.org.project.submissionService.controllers;

import ex.org.project.submissionService.auth.AccessRole;
import ex.org.project.submissionService.auth.UserAuthService;
import ex.org.project.submissionService.models.dtos.StudyRegistrationDTO;
import ex.org.project.submissionService.models.dtos.UserStudyRegistrationDTO;
import ex.org.project.submissionService.services.DataFileService;
import ex.org.project.submissionService.services.StudyRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/study")
public class StudyRegistrationController {

    private final StudyRegistrationService studyRegistrationService;
    private final DataFileService datafileService;
    private final UserAuthService authService;

    @PostMapping("/curator/create")
    public ResponseEntity<Map<String, Integer>> uploadNewStudyAsCurator(@CookieValue(value="chocolateChip", required = false) String sessionId,
                                                               @RequestBody StudyRegistrationDTO studyRegistrationDTO,
                                                               @RequestParam Boolean shouldSubmit) {
        Integer userId = authService.checkAuth(sessionId, List.of(AccessRole.DATA_CURATOR));

        Map<String, Integer> response = studyRegistrationService.registerNewStudy(studyRegistrationDTO, "Curator", userId, shouldSubmit);
        if(shouldSubmit){
            //refresh open search docs so study explorer now includes new study after updates are complete
            studyRegistrationService.triggerOpenSearchRefresh();
        }
      return new ResponseEntity<>(response, HttpStatus.CREATED);

    }

    @PostMapping("/center/create")
    public ResponseEntity<Map<String, Integer>> uploadNewStudy(@CookieValue(value="chocolateChip", required = false) String sessionId,
                                                               @RequestBody StudyRegistrationDTO studyRegistrationDTO,
                                                               @RequestParam Boolean shouldSubmit) {
        Integer userId = authService.checkAuth(sessionId, List.of(AccessRole.DATA_SUBMITTER));
        return new ResponseEntity<>(studyRegistrationService.registerNewStudy(studyRegistrationDTO, "Center", userId, shouldSubmit), HttpStatus.CREATED);
    }

    @GetMapping("/getValues")
    public ResponseEntity<StudyRegistrationDTO> getStudyPropertyValues(@CookieValue(value="chocolateChip", required = false) String sessionId,
                                                                       @RequestParam Integer studyId){
        authService.checkAuth(sessionId, List.of(AccessRole.DATA_SUBMITTER, AccessRole.DATA_CURATOR));
        return ResponseEntity.ok(studyRegistrationService.getStudyProperties(studyId));
    }

    @PutMapping("/curator/edit")
    public ResponseEntity<String> editStudyAsCurator(@CookieValue(value="chocolateChip", required = false) String sessionId,
                                                     @RequestBody StudyRegistrationDTO studyRegistrationDTO,
                                                     @RequestParam Boolean shouldSubmit){
        Integer userId = authService.checkAuth(sessionId, List.of(AccessRole.DATA_CURATOR));
        //TODO: track edits
        String response = studyRegistrationService.editStudyPropertyValues(studyRegistrationDTO, "Curator", shouldSubmit, userId);
        if(shouldSubmit){
            //refresh open search docs so study explorer now includes new study after updates are complete
            studyRegistrationService.triggerOpenSearchRefresh();
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/center/edit")
    public ResponseEntity<String> editStudyAsCenter(@CookieValue(value="chocolateChip", required = false) String sessionId,
                                                    @RequestBody StudyRegistrationDTO studyRegistrationDTO,
                                                    @RequestParam Boolean shouldSubmit){
        Integer userId = authService.checkAuth(sessionId, List.of(AccessRole.DATA_SUBMITTER));
        //TODO: track edits
        String response = studyRegistrationService.editStudyPropertyValues(studyRegistrationDTO, "Center", shouldSubmit, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/center/studies")
    public ResponseEntity<List<UserStudyRegistrationDTO>> getStudiesByCenter(@CookieValue(value="chocolateChip", required = false) String sessionId,
                                                                          @RequestParam("status") String status) {
        Integer userId = authService.checkAuth(sessionId, List.of(AccessRole.DATA_SUBMITTER));
        List<UserStudyRegistrationDTO> studies = studyRegistrationService.getUserStudiesByCenter(userId, status);
        return ResponseEntity.ok(studies);
    }

    @GetMapping("/curator/studies")
    public ResponseEntity<List<UserStudyRegistrationDTO>> getStudiesByCurator(@CookieValue(value="chocolateChip", required = false) String sessionId,
                                                                              @RequestParam("status") String status){
        authService.checkAuth(sessionId, List.of(AccessRole.DATA_CURATOR));
        List<UserStudyRegistrationDTO> studies = studyRegistrationService.getUserStudiesByCurator(status);
        return ResponseEntity.ok(studies);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteStudy(@CookieValue(value="chocolateChip", required = false) String sessionId,
                                                   @RequestParam("studyId") Integer studyId,
                                                  @RequestParam("deleteStudy") Optional<Boolean> deleteStudy) {
        //data submitter is only able to delete the saved(draft) studies
        authService.checkAuth(sessionId, List.of(AccessRole.DATA_CURATOR, AccessRole.DATA_SUBMITTER));
        //by default this function deletes the files and submissions associated with study.
        datafileService.deleteFilesAndSubmissions(studyId);
        log.info("Successfully deleted files and submissions for study id: {}", studyId);
        //if it's flagged to delete study, delete the study and its metadata.
        if(deleteStudy.isPresent() && deleteStudy.get()){
            studyRegistrationService.deleteStudiesByCurator(studyId);
            studyRegistrationService.triggerOpenSearchRefresh();
            log.info("Successfully deleted study id: {}", studyId);
        }
        return ResponseEntity.ok("Successfully deleted study and/or files for study id: " + studyId);
    }


}
