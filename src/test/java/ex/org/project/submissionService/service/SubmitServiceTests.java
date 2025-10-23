package ex.org.project.submissionService.service;

import ex.org.project.submissionService.emails.EmailRequestService;
import ex.org.project.submissionService.models.DataSubmission;
import ex.org.project.submissionService.models.LkupStatus;
import ex.org.project.submissionService.repositories.DataSubmissionRepository;
import ex.org.project.submissionService.repositories.LkupStatusRepository;
import ex.org.project.submissionService.services.SubmissionService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


public class SubmitServiceTests {

    @Mock
    private LkupStatusRepository lkupStatusRepository;

    @Mock
    private DataSubmissionRepository dataSubmissionRepository;

    @Mock
    private EmailRequestService emailRequestService;

    @InjectMocks
    private SubmissionService submissionService;

    public SubmitServiceTests() {
        MockitoAnnotations.initMocks(this);
    }


    @Test
    public void testSubmit_ExistingSubmission_Successful() {
        Integer submissionId = 16;
        LkupStatus lkupStatus = new LkupStatus();
        lkupStatus.setId(1);
        lkupStatus.setName("submitted");
        DataSubmission submission = new DataSubmission();
        submission.setId(submissionId);

        when(lkupStatusRepository.findByUsageAndName("data_submission", "submitted")).thenReturn(Optional.of(lkupStatus));
        when(dataSubmissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

        // Invoke the actual method being tested
        Boolean result = submissionService.submit(submissionId);

        assertTrue(result);
        assertEquals(lkupStatus.getId(), submission.getStatusId());

        // Verify the method invocations
        verify(lkupStatusRepository, times(1)).findByUsageAndName("data_submission", "submitted");
        verify(dataSubmissionRepository, times(1)).findById(submissionId);
    }

    @Test
    public void testSubmit_NonExistingSubmission_ExceptionThrown() {
        Integer submissionId = 999;
        LkupStatus lkupStatus = new LkupStatus();
        lkupStatus.setId(1);
        lkupStatus.setName("submitted");
        when(lkupStatusRepository.findByUsageAndName("data_submission", "submitted")).thenReturn(Optional.of(lkupStatus));
        when(dataSubmissionRepository.findById(submissionId)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> {
            submissionService.submit(submissionId);
        });
    }

}
