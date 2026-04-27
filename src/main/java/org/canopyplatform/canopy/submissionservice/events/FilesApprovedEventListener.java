package org.canopyplatform.canopy.submissionservice.events;

import org.canopyplatform.canopy.submissionservice.services.StudyRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for FilesApprovedEvent and triggers OpenSearch refresh after transaction commits
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FilesApprovedEventListener {

    private final StudyRegistrationService studyRegistrationService;

    /**
     * Handles file approval events after the transaction commits.
     * This ensures OpenSearch indexing happens only after database changes are persisted.
     *
     * @param event The files approved event containing study information
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleFilesApproved(FilesApprovedEvent event) {
        log.info("Transaction committed. Processing FilesApprovedEvent for studyId={}, approvedFiles={}",
                event.getStudyId(), event.getApprovedFileCount());

        try {
            studyRegistrationService.triggerOpenSearchRefresh();
            log.info("OpenSearch refresh triggered successfully for studyId={}", event.getStudyId());
        } catch (Exception e) {
            log.error("Error triggering OpenSearch refresh for studyId={}", event.getStudyId(), e);
        }
    }
}

