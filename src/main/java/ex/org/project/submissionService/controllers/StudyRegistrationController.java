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

    @PostMapping("/create")
    public ResponseEntity<Map<String, Integer>> uploadNewStudy(@CookieValue(value="chocolateChip", required = false) String sessionId,
                                                               @RequestParam("file") MultipartFile file) {
        Integer userId = authService.checkAuth(sessionId, List.of(AccessRole.DATA_CURATOR));
        return new ResponseEntity<>(studyRegistrationService.registerNewStudy(file, userId), HttpStatus.CREATED);
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
        return ResponseEntity.ok(response);
    }

    @PutMapping("/dcc/edit")
    public ResponseEntity<String> editStudyAsDcc(@CookieValue(value="chocolateChip", required = false) String sessionId,
                                                 @RequestBody StudyRegistrationDTO studyRegistrationDTO,
                                                 @RequestParam Boolean shouldSubmit){
        Integer userId = authService.checkAuth(sessionId, List.of(AccessRole.DATA_SUBMITTER));
        //TODO: track edits
        String response = studyRegistrationService.editStudyPropertyValues(studyRegistrationDTO, "DCC", shouldSubmit, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dcc/studies")
    public ResponseEntity<List<UserStudyRegistrationDTO>> getStudiesByDcc(@CookieValue(value="chocolateChip", required = false) String sessionId,
                                                                          @RequestParam("status") String status) {
        Integer userId = authService.checkAuth(sessionId, List.of(AccessRole.DATA_SUBMITTER));
        List<UserStudyRegistrationDTO> studies = studyRegistrationService.getUserStudiesByDcc(userId, status);
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
        authService.checkAuth(sessionId, List.of(AccessRole.DATA_CURATOR));
        //by default this function deletes the files and submissions associated with study.
        datafileService.deleteFilesAndSubmissions(studyId);
        log.info("Successfully deleted files and submissions for study id: " + studyId);
        //if it's flagged to delete study, delete the study and its metadata.
        if(deleteStudy.isPresent() && deleteStudy.get()){
            studyRegistrationService.deleteStudiesByCurator(studyId);
            log.info("Successfully deleted study id: " + studyId);
        }
        return ResponseEntity.ok("Successfully deleted study and/or files for study id: " + studyId);
    }


}
