package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.dtos.StudiesDTO;
import ex.org.project.submissionService.models.StudyPropertyValue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StudyMapper {
	
	@Mapping(source = "propertyValue", target = "dcc")
	StudiesDTO toDTO(StudyPropertyValue study);
    
    List<StudiesDTO> toDTOs(List<StudyPropertyValue> studies);
}
