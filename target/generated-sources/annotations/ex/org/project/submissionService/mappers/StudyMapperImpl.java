package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.StudyPropertyValue;
import ex.org.project.submissionService.models.dtos.StudiesDTO;
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
public class StudyMapperImpl implements StudyMapper {

    @Override
    public StudiesDTO toDTO(StudyPropertyValue study) {
        if ( study == null ) {
            return null;
        }

        String dcc = null;
        Integer studyId = null;

        dcc = study.getPropertyValue();
        studyId = study.getStudyId();

        StudiesDTO studiesDTO = new StudiesDTO( studyId, dcc );

        return studiesDTO;
    }

    @Override
    public List<StudiesDTO> toDTOs(List<StudyPropertyValue> studies) {
        if ( studies == null ) {
            return null;
        }

        List<StudiesDTO> list = new ArrayList<StudiesDTO>( studies.size() );
        for ( StudyPropertyValue studyPropertyValue : studies ) {
            list.add( toDTO( studyPropertyValue ) );
        }

        return list;
    }
}
