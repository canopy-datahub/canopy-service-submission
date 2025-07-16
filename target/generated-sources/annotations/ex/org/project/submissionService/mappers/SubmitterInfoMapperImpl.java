package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.DataSubmission;
import ex.org.project.submissionService.models.LkupStatus;
import ex.org.project.submissionService.models.dtos.SubmissionInfoDTO;
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
public class SubmitterInfoMapperImpl implements SubmitterInfoMapper {

    @Override
    public SubmissionInfoDTO toDTO(DataSubmission dataSubmission) {
        if ( dataSubmission == null ) {
            return null;
        }

        SubmissionInfoDTO submissionInfoDTO = new SubmissionInfoDTO();

        submissionInfoDTO.setId( dataSubmission.getId() );
        submissionInfoDTO.setStudyId( dataSubmission.getStudyId() );
        submissionInfoDTO.setStudyName( prependPhs( dataSubmission.getStudy() ) );
        submissionInfoDTO.setStatus( dataSubmissionStatusName( dataSubmission ) );
        submissionInfoDTO.setCreatedDate( dataSubmission.getCreatedAt() );
        submissionInfoDTO.setSubmittedDate( dataSubmission.getDateSubmitted() );
        submissionInfoDTO.setModifiedDate( dataSubmission.getModifiedAt() );

        return submissionInfoDTO;
    }

    @Override
    public List<SubmissionInfoDTO> toDTOList(List<DataSubmission> dataSubmissions) {
        if ( dataSubmissions == null ) {
            return null;
        }

        List<SubmissionInfoDTO> list = new ArrayList<SubmissionInfoDTO>( dataSubmissions.size() );
        for ( DataSubmission dataSubmission : dataSubmissions ) {
            list.add( toDTO( dataSubmission ) );
        }

        return list;
    }

    private String dataSubmissionStatusName(DataSubmission dataSubmission) {
        if ( dataSubmission == null ) {
            return null;
        }
        LkupStatus status = dataSubmission.getStatus();
        if ( status == null ) {
            return null;
        }
        String name = status.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }
}
