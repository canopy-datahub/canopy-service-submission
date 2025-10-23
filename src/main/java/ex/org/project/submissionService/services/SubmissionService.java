package ex.org.project.submissionService.services;

import ex.org.project.datahub.auth.exception.UserNotFoundException;
import ex.org.project.submissionService.emails.DataIngestEmailType;
import ex.org.project.submissionService.emails.EmailRequestService;
import ex.org.project.submissionService.exceptions.custom.StatusNotFoundException;
import ex.org.project.submissionService.exceptions.custom.StudyPropertyValuesRetrievalException;
import ex.org.project.submissionService.exceptions.custom.SubmitterCenterException;
import ex.org.project.submissionService.mappers.ViewStudyMapper;
import ex.org.project.submissionService.models.*;
import ex.org.project.submissionService.models.dtos.StudiesDTO;
import ex.org.project.submissionService.repositories.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

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
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(String.format("User ID %d not found", userId)));
        if(user.getCenter() == null || user.getCenter().getName() == null) {
            throw new SubmitterCenterException("Please contact support for center alignment");
        }
        List<ViewStudy> studies = viewStudyRepository.findAllByCenterWithoutInProgressSubmissions(user.getCenter().getName());
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
