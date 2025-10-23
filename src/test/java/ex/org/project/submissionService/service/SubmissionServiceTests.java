package ex.org.project.submissionService.service;

import ex.org.project.submissionService.emails.EmailRequestService;
import ex.org.project.submissionService.exceptions.custom.SubmitterCenterException;
import ex.org.project.submissionService.mappers.ViewStudyMapper;
import ex.org.project.submissionService.models.*;
import ex.org.project.submissionService.models.dtos.StudiesDTO;
import ex.org.project.submissionService.repositories.*;
import ex.org.project.submissionService.services.SubmissionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class SubmissionServiceTests {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private DataSubmissionRepository dataSubmissionRepository;

    @Mock
    private LkupStatusRepository lkupStatusRepository;

    @Mock
    private ViewStudyRepository viewStudyRepository;

    @Mock
    private LkupSubmissionStepRepository lkupSubmissionStepRepository;

    @Mock
    private EmailRequestService emailRequestService;

    @InjectMocks
    private SubmissionService submissionService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        ViewStudyMapper viewStudyMapper = Mappers.getMapper(ViewStudyMapper.class);
        submissionService = new SubmissionService(usersRepository, dataSubmissionRepository, lkupStatusRepository,
                                                  viewStudyRepository, viewStudyMapper, lkupSubmissionStepRepository,
                                                  emailRequestService);
    }

    @Test
    void getStudiesByUserDcc_HappyPath() {
        Integer userId = 1;
        Users user = new Users();
        user.setId(userId);
        LkupCenter dcc = new LkupCenter();
        dcc.setId(3);
        dcc.setName("test");
        user.setCenter(dcc);

        ViewStudy vstudy = new ViewStudy();
        vstudy.setStudyName("Test Study Name");
        vstudy.setPhs("TestPhsNumber");
        vstudy.setStudyId(10);
        List<ViewStudy> viewStudyList = List.of(vstudy);

        when(usersRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(viewStudyRepository.findAllByCenterWithoutInProgressSubmissions("test"))
                .thenReturn(viewStudyList);

        List<StudiesDTO> result = submissionService.getStudiesByUserCenter(userId);

        Assertions.assertFalse(result.isEmpty());
        StudiesDTO dto = result.get(0);
        Assertions.assertEquals("(TestPhsNumber) Test Study Name", dto.getCenter());
    }

    @Test
    void getStudiesByUserDcc_UserHasNullDcc() {
        Integer userId = 1;
        Users user = new Users();
        user.setId(userId);

        when(usersRepository.findById(userId))
                .thenReturn(Optional.of(user));

        assertThrows(SubmitterCenterException.class, () -> submissionService.getStudiesByUserCenter(userId));
    }

    @Test
    void testCreateStudy_Success() {
        int studyId = 123;
        int statusId = 1;
        int submissionId = 1;

        LkupStatus initiatedStatus = new LkupStatus();
        initiatedStatus.setId(statusId);
        when(lkupStatusRepository.findByUsageAndName("data_submission", "initiated"))
                .thenReturn(Optional.of(initiatedStatus));

        DataSubmission createdSubmission = new DataSubmission(studyId, 1, initiatedStatus);
        DataSubmission savedSubmission = new DataSubmission();
        savedSubmission.setId(submissionId);
        savedSubmission.setStudyId(studyId);
        when(dataSubmissionRepository.save(createdSubmission)).thenReturn(savedSubmission);

        assertEquals(submissionId, savedSubmission.getId());
    }
}
