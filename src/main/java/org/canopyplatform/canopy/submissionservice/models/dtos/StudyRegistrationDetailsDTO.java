package org.canopyplatform.canopy.submissionservice.models.dtos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.canopyplatform.canopy.submissionservice.models.AccessLevel;

public record StudyRegistrationDetailsDTO(
        Integer studyId,
        List<StudyPropertyValueDTO> studyPropertyValues,
        String status,
        @JsonProperty("access_level") AccessLevel accessLevel
) {
}
