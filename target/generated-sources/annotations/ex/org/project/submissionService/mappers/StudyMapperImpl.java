package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.StudyPropertyValue;
import ex.org.project.submissionService.models.dtos.StudiesDTO;
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
