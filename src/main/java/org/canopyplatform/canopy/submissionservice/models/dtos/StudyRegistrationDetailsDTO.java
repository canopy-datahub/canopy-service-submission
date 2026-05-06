package org.canopyplatform.canopy.submissionservice.models.dtos;

import java.util.List;

public record StudyRegistrationDetailsDTO(
        Integer studyId,
        List<StudyPropertyValueDTO> studyPropertyValues,
        String status
) {
}
