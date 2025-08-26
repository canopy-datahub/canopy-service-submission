package ex.org.project.submissionService.mappers;


import ex.org.project.submissionService.models.ViewStudy;
import ex.org.project.submissionService.models.dtos.UserStudyRegistrationDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;
@Mapper(componentModel = "spring")
public interface ViewStudyDccMapper {

    @Mapping(source = "study.submissionStatus", target = "status")
    UserStudyRegistrationDTO toDTO(ViewStudy study);

    List<UserStudyRegistrationDTO> toDTOs(List<ViewStudy> studies);

}