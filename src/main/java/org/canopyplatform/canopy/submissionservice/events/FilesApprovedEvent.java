package org.canopyplatform.canopy.submissionservice.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when files are approved and need OpenSearch indexing
 */
@Getter
public class FilesApprovedEvent extends ApplicationEvent {
    private final Integer studyId;
    private final int approvedFileCount;

    public FilesApprovedEvent(Object source, Integer studyId, int approvedFileCount) {
        super(source);
        this.studyId = studyId;
        this.approvedFileCount = approvedFileCount;
    }
}

