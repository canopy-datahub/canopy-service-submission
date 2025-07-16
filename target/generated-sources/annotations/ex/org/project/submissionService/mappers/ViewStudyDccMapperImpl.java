package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.ViewStudy;
import ex.org.project.submissionService.models.dtos.UserStudyRegistrationDTO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-07-15T14:01:27-0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.14 (Oracle Corporation)"
)
@Component
public class ViewStudyDccMapperImpl implements ViewStudyDccMapper {

    @Override
    public UserStudyRegistrationDTO toDTO(ViewStudy study) {
        if ( study == null ) {
            return null;
        }

        UserStudyRegistrationDTO userStudyRegistrationDTO = new UserStudyRegistrationDTO();

        userStudyRegistrationDTO.setStatus( study.getSubmissionStatus() );
        userStudyRegistrationDTO.setStudyId( study.getStudyId() );
        userStudyRegistrationDTO.setPhs( study.getPhs() );
        userStudyRegistrationDTO.setStudyName( study.getStudyName() );
        userStudyRegistrationDTO.setCreatedAt( study.getCreatedAt() );
        userStudyRegistrationDTO.setHasDataFiles( study.getHasDataFiles() );

        return userStudyRegistrationDTO;
    }

    @Override
    public List<UserStudyRegistrationDTO> toDTOs(List<ViewStudy> studies) {
        if ( studies == null ) {
            return null;
        }

        List<UserStudyRegistrationDTO> list = new ArrayList<UserStudyRegistrationDTO>( studies.size() );
        for ( ViewStudy viewStudy : studies ) {
            list.add( toDTO( viewStudy ) );
        }

        return list;
    }
}
