package org.canopyplatform.canopy.submissionservice.models.dtos;

import java.util.List;

public record StudyRegistrationDTO(
        Integer studyId,
        List<StudyPropertyValueDTO> studyPropertyValues
) {
}
