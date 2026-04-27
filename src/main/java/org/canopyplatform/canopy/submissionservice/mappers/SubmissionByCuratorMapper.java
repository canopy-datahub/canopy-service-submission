package org.canopyplatform.canopy.submissionservice.mappers;

import org.canopyplatform.canopy.submissionservice.models.ViewStudy;
import org.canopyplatform.canopy.submissionservice.models.dtos.DetailsDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubmissionByCuratorMapper {


   DetailsDTO toDTOs(ViewStudy viewStudies);
}
