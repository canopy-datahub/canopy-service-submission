package org.canopyplatform.canopy.submissionservice.mappers;

import org.canopyplatform.canopy.submissionservice.models.dtos.StudiesDTO;
import org.canopyplatform.canopy.submissionservice.models.ViewStudy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ViewStudyMapper {
    @Mapping(target = "center", expression = "java(study.getStudyIdTitle())")
    StudiesDTO toDTO(ViewStudy study);

    List<StudiesDTO> toDTOs(List<ViewStudy> studies);

}
