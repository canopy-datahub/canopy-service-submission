package org.canopyplatform.canopy.submissionservice.models.dtos;

import java.util.List;

import org.canopyplatform.canopy.submissionservice.models.AccessLevel;

public record StudyRegistrationDetailsDTO(
        Integer studyId,
        List<StudyPropertyValueDTO> studyPropertyValues,
        String status,
        AccessLevel accessLevel
) {
}
