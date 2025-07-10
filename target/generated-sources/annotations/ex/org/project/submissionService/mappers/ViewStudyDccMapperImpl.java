package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.ViewStudy;
import ex.org.project.submissionService.models.dtos.UserStudyRegistrationDTO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-07-09T16:48:24-0700",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.50.v20250628-1110, environment: Java 21.0.7 (Eclipse Adoptium)"
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
        userStudyRegistrationDTO.setCreatedAt( study.getCreatedAt() );
        userStudyRegistrationDTO.setHasDataFiles( study.getHasDataFiles() );
        userStudyRegistrationDTO.setPhs( study.getPhs() );
        userStudyRegistrationDTO.setStudyId( study.getStudyId() );
        userStudyRegistrationDTO.setStudyName( study.getStudyName() );

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
