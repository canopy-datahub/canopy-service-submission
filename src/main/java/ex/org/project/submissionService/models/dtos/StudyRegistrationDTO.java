package ex.org.project.submissionService.models.dtos;

import java.util.List;

public record StudyRegistrationDTO(
        Integer studyId,
        List<StudyPropertyValueDTO> studyPropertyValues
) {
}
