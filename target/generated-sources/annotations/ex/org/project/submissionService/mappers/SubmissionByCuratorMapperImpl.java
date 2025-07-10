package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.ViewStudy;
import ex.org.project.submissionService.models.dtos.DetailsDTO;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-07-09T16:48:24-0700",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.50.v20250628-1110, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class SubmissionByCuratorMapperImpl implements SubmissionByCuratorMapper {

    @Override
    public DetailsDTO toDTOs(ViewStudy viewStudies) {
        if ( viewStudies == null ) {
            return null;
        }

        DetailsDTO detailsDTO = new DetailsDTO();

        detailsDTO.setPhs( viewStudies.getPhs() );
        detailsDTO.setStudyName( viewStudies.getStudyName() );

        return detailsDTO;
    }
}
