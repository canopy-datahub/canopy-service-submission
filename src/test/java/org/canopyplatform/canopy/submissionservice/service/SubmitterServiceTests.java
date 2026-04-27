
package org.canopyplatform.canopy.submissionservice.service;

import org.canopyplatform.canopy.submissionservice.mappers.SubmitterInfoMapper;
import org.canopyplatform.canopy.submissionservice.exceptions.custom.DataFileNotFoundException;
import org.canopyplatform.canopy.submissionservice.models.*;
import org.canopyplatform.canopy.submissionservice.models.*;
import org.canopyplatform.canopy.submissionservice.models.dtos.SubmissionInfoDTO;
import org.canopyplatform.canopy.submissionservice.repositories.DataFileRepository;
import org.canopyplatform.canopy.submissionservice.repositories.DataSubmissionRepository;
import org.canopyplatform.canopy.submissionservice.repositories.LkupStatusRepository;
import org.canopyplatform.canopy.submissionservice.services.DataFileService;
import org.canopyplatform.canopy.submissionservice.services.SubmitterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

public class SubmitterServiceTests {

    @Mock
    private DataSubmissionRepository dataSubmissionRepository;

    @Mock
    private DataFileRepository  dataFileRepository;

    @Mock
    private DataFileService dataFileService;

    @Mock
    private SubmitterInfoMapper submitterInfoMapper;

    private SubmitterService submitterService;

    @Mock
    private LkupStatusRepository lkupStatusRepository;


    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        submitterService = new SubmitterService(submitterInfoMapper,dataSubmissionRepository,dataFileRepository,dataFileService, lkupStatusRepository);
    }

    @Test
    public void testGetSubmissions() {
        // Mock data
        Users user = new Users();
        user.setId(1);

        DataSubmission submission1 = new DataSubmission();
        submission1.setId(101);
        submission1.setSubmitterUserId(user.getId());

        DataSubmission submission2 = new DataSubmission();
        submission2.setId(102);
        submission2.setSubmitterUserId(user.getId());

        List<DataSubmission> submissionList = Arrays.asList(submission1, submission2);

        SubmissionInfoDTO submissionInfoDTO1 = new SubmissionInfoDTO();
        SubmissionInfoDTO submissionInfoDTO2 = new SubmissionInfoDTO();

        LkupStatus status = new LkupStatus();
        status.setId(1);

        when(dataSubmissionRepository.findBySubmitterUserIdAndStatusOrderByModifiedAtDesc( 1, status )).thenReturn(submissionList);
        when(submitterInfoMapper.toDTO(submission1)).thenReturn(submissionInfoDTO1);
        when(submitterInfoMapper.toDTO(submission2)).thenReturn(submissionInfoDTO2);

        when(lkupStatusRepository.findByUsageAndName("data_submission", "in_review")).thenReturn(Optional.of(status));

        List<SubmissionInfoDTO> result = submitterService.getSubmissions(1, "in_review");

        // Verify the results
        assertEquals(2, result.size());
        assertEquals(submissionInfoDTO1, result.get(0));
        assertEquals(submissionInfoDTO2, result.get(1));

        // Verify method invocations
        verify(dataSubmissionRepository, times(1))
                .findBySubmitterUserIdAndStatusOrderByModifiedAtDesc(user.getId(), status);
        verify(submitterInfoMapper, times(2)).toDTO(any(DataSubmission.class));
    }

    @Test
    void deleteSubmission_shouldDeleteSubmissionAndFiles() {
        int submissionId = 1;
        DataSubmission dataSubmission = new DataSubmission();
        dataSubmission.setId(submissionId);
        DataFile dataFile1 = new DataFile();
        dataFile1.setId(1);
        S3File s3File1 = new S3File();
        dataFile1.setS3File(s3File1);

        List<DataFile> datafileList = Collections.singletonList(dataFile1);

        when(dataSubmissionRepository.findById(submissionId)).thenReturn(Optional.of(dataSubmission));
        when(dataFileRepository.findBySubmissionId(dataSubmission.getId())).thenReturn(datafileList);

        assertDoesNotThrow(() -> submitterService.deleteSubmission(submissionId));

        verify(dataFileRepository, times(1)).deleteById(dataFile1.getId());
        verify(dataFileService, times(1)).deleteS3FileAndEntity(s3File1);
        verify(dataSubmissionRepository, times(1)).deleteById(submissionId);
    }

    @Test
    void deleteSubmission_shouldThrowExceptionIfNoFilesFound() {

        int submissionId = 1;
        DataSubmission dataSubmission = new DataSubmission();
        dataSubmission.setId(submissionId);

        when(dataSubmissionRepository.findById(submissionId)).thenReturn(Optional.of(dataSubmission));
        when(dataFileRepository.findBySubmissionId(dataSubmission.getId())).thenReturn(Collections.emptyList());

        assertThrows(DataFileNotFoundException.class, () -> submitterService.deleteSubmission(submissionId));
    }

}

