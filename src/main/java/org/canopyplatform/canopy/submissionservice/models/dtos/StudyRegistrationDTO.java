package org.canopyplatform.canopy.submissionservice.models.dtos;

import java.util.List;

import org.canopyplatform.canopy.submissionservice.models.AccessLevel;

public record StudyRegistrationDTO(
        Integer studyId,
        List<StudyPropertyValueDTO> studyPropertyValues,
        AccessLevel accessLevel
) {
    /**
     * Legacy two-arg shape preserved so older callers (tests, scripted JSON
     * clients) that don't pass accessLevel still compile and behave the same.
     * A null accessLevel means "do not change" for edits and "default to
     * PUBLIC" for new studies — handled in StudyRegistrationService.
     */
    public StudyRegistrationDTO(Integer studyId, List<StudyPropertyValueDTO> studyPropertyValues) {
        this(studyId, studyPropertyValues, null);
    }
}
