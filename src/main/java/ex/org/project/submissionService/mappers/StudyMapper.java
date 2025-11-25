package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.StudyPropertyValue;
import ex.org.project.submissionService.models.dtos.StudiesDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StudyMapper {

	@Mapping(source = "propertyValue", target = "center")
	StudiesDTO toDTO(StudyPropertyValue study);

    List<StudiesDTO> toDTOs(List<StudyPropertyValue> studies);
}
