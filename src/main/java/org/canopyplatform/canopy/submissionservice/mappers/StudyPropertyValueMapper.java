package org.canopyplatform.canopy.submissionservice.mappers;

import org.canopyplatform.canopy.submissionservice.models.EntityProperty;
import org.canopyplatform.canopy.submissionservice.models.StudyPropertyValue;
import org.canopyplatform.canopy.submissionservice.models.dtos.EntityPropertyDTO;
import org.canopyplatform.canopy.submissionservice.models.dtos.StudyPropertyValueDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StudyPropertyValueMapper {

    @Mapping(target = "shouldBeRemoved", constant = "false")
    @Mapping(target = "value", source = "propertyValue")
    StudyPropertyValueDTO entityToDto(StudyPropertyValue spv);

    @Mapping(target = "propertyValue", source = "value")
    StudyPropertyValue DtoToEntity(StudyPropertyValueDTO spvDto);

    EntityPropertyDTO entityPropertyToDto(EntityProperty entityProperty);

    EntityProperty dtoToEntityProperty(EntityPropertyDTO entityPropertyDto);

    List<StudyPropertyValueDTO> entityListToDtoList(List<StudyPropertyValue> spvList);

    List<StudyPropertyValue> dtoListToEntityList(List<StudyPropertyValueDTO> dtoList);
}
