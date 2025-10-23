package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.DataSubmission;
import ex.org.project.submissionService.models.ViewStudy;
import ex.org.project.submissionService.models.dtos.DataSubmissionDTO;
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
