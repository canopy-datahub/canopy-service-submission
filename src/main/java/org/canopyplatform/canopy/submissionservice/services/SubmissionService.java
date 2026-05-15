package org.canopyplatform.canopy.submissionservice.services;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.canopyplatform.canopy.submissionservice.emails.DataIngestEmailType;
import org.canopyplatform.canopy.submissionservice.emails.EmailRequestService;
import org.canopyplatform.canopy.submissionservice.exceptions.custom.StatusNotFoundException;
import org.canopyplatform.canopy.submissionservice.mappers.ViewStudyMapper;
import org.canopyplatform.canopy.submissionservice.models.*;
import org.canopyplatform.canopy.submissionservice.repositories.*;
import org.springframework.stereotype.Service;

import org.canopyplatform.canopy.submissionservice.exceptions.custom.StudyPropertyValuesRetrievalException;
import org.canopyplatform.canopy.submissionservice.models.dtos.StudiesDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class SubmissionService {

    private final UsersRepository usersRepository;
    private final DataSubmissionRepository dataSubmissionRepository;
    private final LkupStatusRepository lkupStatusRepository;
    private final ViewStudyRepository viewStudyRepository;
    private final ViewStudyMapper viewStudyMapper;
    private final LkupSubmissionStepRepository lkupSubmissionStepRepository;
    private final EmailRequestService emailRequestService;

    public List<StudiesDTO> getStudiesByUserCenter(Integer userId) throws StudyPropertyValuesRetrievalException{
        // The method name predates Creator authorization. The dropdown now
        // surfaces "studies I created" (and that are Approved + free of an
        // in-progress submission). Center membership is no longer a gate.
        List<ViewStudy> studies = viewStudyRepository.findAllByCreatorWithoutInProgressSubmissions(userId);
        return viewStudyMapper.toDTOs(studies);
    }
    /**
     * Creates a new data submission in progress for a given study.
     * It sets the status to "in_progress", creates a new DataSubmission object,
     * sets the creation timestamp, submission step, and saves the submission.
     * @param studyId The ID of the study for which the submission is created
     * @param userId ID of the user creating the submission
     * @return The ID of the newly created submission
     */
	public Integer createSubmission(Integer studyId, Integer userId) {

		LkupStatus status = lkupStatusRepository.findByUsageAndName(Constants.USAGE_DATA_SUBMISSION, Constants.STATUS_IN_PROGRESS)
                .orElseThrow(()  -> new StatusNotFoundException(String.format("Could not find submission status entity. Invalid Data Submission Status: %s", Constants.STATUS_IN_PROGRESS)));

		DataSubmission submission = new DataSubmission(studyId, userId, status);
        Timestamp creationDate = Timestamp.valueOf(LocalDateTime.now());
        submission.setCreatedAt(creationDate);
        submission.setModifiedAt(creationDate);
        submission.setModifiedBy(userId);
        submission.setStepId(lkupSubmissionStepRepository.findByDescription("Upload Files").get());
        submission = dataSubmissionRepository.save(submission);
		return submission.getId();
	}

    /**
     * Marks a data submission as submitted by updating its status to "submitted".
     * It saves the updated submission and returns true.
     */
    @Transactional
    public Boolean submit(Integer submissionId) {
        LkupStatus lkupStatus = lkupStatusRepository.findByUsageAndName(Constants.USAGE_DATA_SUBMISSION, Constants.STATUS_SUBMITTED)
                .orElseThrow(()  -> new StatusNotFoundException(String.format("Could not find submission status entity. Invalid Data Submission Status: %s", Constants.STATUS_SUBMITTED)));
        DataSubmission submission = dataSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new NoSuchElementException("Submission not found with ID: " + submissionId));

        submission.setStatusId(lkupStatus.getId());
        submission.setDateSubmitted(new Timestamp(System.currentTimeMillis()));
        dataSubmissionRepository.save(submission);
        emailRequestService.sendDataIngestEmail(submissionId, DataIngestEmailType.SUBMISSION_CONFIRMATION);
        return true;
    }

}
