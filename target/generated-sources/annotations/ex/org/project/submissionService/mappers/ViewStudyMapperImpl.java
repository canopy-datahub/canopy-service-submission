package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.ViewStudy;
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
public class ViewStudyMapperImpl implements ViewStudyMapper {

    @Override
    public StudiesDTO toDTO(ViewStudy study) {
        if ( study == null ) {
            return null;
        }

        Integer studyId = null;

        studyId = study.getStudyId();

        String dcc = study.getPhsTitle();

        StudiesDTO studiesDTO = new StudiesDTO( studyId, dcc );

        return studiesDTO;
    }

    @Override
    public List<StudiesDTO> toDTOs(List<ViewStudy> studies) {
        if ( studies == null ) {
            return null;
        }

        List<StudiesDTO> list = new ArrayList<StudiesDTO>( studies.size() );
        for ( ViewStudy viewStudy : studies ) {
            list.add( toDTO( viewStudy ) );
        }

        return list;
    }
}
