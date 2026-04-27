package org.canopyplatform.canopy.submissionservice.mappers;
import org.canopyplatform.canopy.submissionservice.models.DataSubmission;
import org.canopyplatform.canopy.submissionservice.models.ViewStudy;
import org.canopyplatform.canopy.submissionservice.models.dtos.DataSubmissionDTO;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;
@Mapper(componentModel = "spring")
public interface DataSubmissionMapper {

    DataSubmissionDTO toDTO(ViewStudy viewStudy);
    List<DataSubmissionDTO> toDTOs(List<ViewStudy> viewStudy);

    @Mapping(source = "viewStudy.studyId", target = "studyId")
    @Mapping(source = "dataSubmission.createdAt", target = "createdAt")
    @Mapping(source = "dataSubmission.dateSubmitted", target = "submissionDate")
    @Mapping(source = "dataSubmission.status.name", target = "submissionStatus")
    DataSubmissionDTO toDTO(ViewStudy viewStudy, DataSubmission dataSubmission);
}
