package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.ViewStudy;
import ex.org.project.submissionService.models.dtos.DetailsDTO;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-07-15T14:01:27-0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.14 (Oracle Corporation)"
)
@Component
public class SubmissionByCuratorMapperImpl implements SubmissionByCuratorMapper {

    @Override
    public DetailsDTO toDTOs(ViewStudy viewStudies) {
        if ( viewStudies == null ) {
            return null;
        }

        DetailsDTO detailsDTO = new DetailsDTO();

        detailsDTO.setStudyName( viewStudies.getStudyName() );
        detailsDTO.setPhs( viewStudies.getPhs() );

        return detailsDTO;
    }
}
