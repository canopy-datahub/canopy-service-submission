package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.ViewStudy;
import ex.org.project.submissionService.models.dtos.DetailsDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubmissionByCuratorMapper {


   DetailsDTO toDTOs(ViewStudy viewStudies);
}
