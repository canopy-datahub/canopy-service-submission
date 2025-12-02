package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.dtos.StudiesDTO;
import ex.org.project.submissionService.models.ViewStudy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ViewStudyMapper {
    @Mapping(target = "center", expression = "java(study.getStudyIdTitle())")
    StudiesDTO toDTO(ViewStudy study);

    List<StudiesDTO> toDTOs(List<ViewStudy> studies);

}
