package org.canopyplatform.canopy.submissionservice.mappers;

import org.canopyplatform.canopy.submissionservice.models.dtos.StudiesDTO;
import org.canopyplatform.canopy.submissionservice.models.StudyPropertyValue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StudyMapper {

	@Mapping(source = "propertyValue", target = "center")
	StudiesDTO toDTO(StudyPropertyValue study);

    List<StudiesDTO> toDTOs(List<StudyPropertyValue> studies);
}
