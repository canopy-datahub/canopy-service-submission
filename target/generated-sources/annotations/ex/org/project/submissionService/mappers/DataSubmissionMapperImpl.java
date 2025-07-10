package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.DataSubmission;
import ex.org.project.submissionService.models.LkupStatus;
import ex.org.project.submissionService.models.ViewStudy;
import ex.org.project.submissionService.models.dtos.DataSubmissionDTO;
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
public class DataSubmissionMapperImpl implements DataSubmissionMapper {

    @Override
    public DataSubmissionDTO toDTO(ViewStudy viewStudy) {
        if ( viewStudy == null ) {
            return null;
        }

        DataSubmissionDTO dataSubmissionDTO = new DataSubmissionDTO();

        dataSubmissionDTO.setCreatedAt( viewStudy.getCreatedAt() );
        dataSubmissionDTO.setDCC( viewStudy.getDCC() );
        dataSubmissionDTO.setPhs( viewStudy.getPhs() );
        dataSubmissionDTO.setStudyId( viewStudy.getStudyId() );
        dataSubmissionDTO.setStudyName( viewStudy.getStudyName() );
        dataSubmissionDTO.setSubmissionStatus( viewStudy.getSubmissionStatus() );

        return dataSubmissionDTO;
    }

    @Override
    public List<DataSubmissionDTO> toDTOs(List<ViewStudy> viewStudy) {
        if ( viewStudy == null ) {
            return null;
        }

        List<DataSubmissionDTO> list = new ArrayList<DataSubmissionDTO>( viewStudy.size() );
        for ( ViewStudy viewStudy1 : viewStudy ) {
            list.add( toDTO( viewStudy1 ) );
        }

        return list;
    }

    @Override
    public DataSubmissionDTO toDTO(ViewStudy viewStudy, DataSubmission dataSubmission) {
        if ( viewStudy == null && dataSubmission == null ) {
            return null;
        }

        DataSubmissionDTO dataSubmissionDTO = new DataSubmissionDTO();

        if ( viewStudy != null ) {
            dataSubmissionDTO.setStudyId( viewStudy.getStudyId() );
            dataSubmissionDTO.setDCC( viewStudy.getDCC() );
            dataSubmissionDTO.setPhs( viewStudy.getPhs() );
            dataSubmissionDTO.setStudyName( viewStudy.getStudyName() );
        }
        if ( dataSubmission != null ) {
            dataSubmissionDTO.setCreatedAt( dataSubmission.getCreatedAt() );
            dataSubmissionDTO.setSubmissionDate( dataSubmission.getDateSubmitted() );
            dataSubmissionDTO.setSubmissionStatus( dataSubmissionStatusName( dataSubmission ) );
            dataSubmissionDTO.setId( dataSubmission.getId() );
        }

        return dataSubmissionDTO;
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
