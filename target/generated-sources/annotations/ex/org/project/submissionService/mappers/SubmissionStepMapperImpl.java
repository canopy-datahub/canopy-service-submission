package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.DataSubmission;
import ex.org.project.submissionService.models.LkupSubmissionStep;
import ex.org.project.submissionService.models.Study;
import ex.org.project.submissionService.models.dtos.SubmissionStepDTO;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-07-09T16:48:24-0700",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.50.v20250628-1110, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class SubmissionStepMapperImpl implements SubmissionStepMapper {

    @Override
    public SubmissionStepDTO toStepDto(LkupSubmissionStep lkupSubmissionStep, DataSubmission dataSubmission, Study study) {
        if ( lkupSubmissionStep == null && dataSubmission == null && study == null ) {
            return null;
        }

        SubmissionStepDTO submissionStepDTO = new SubmissionStepDTO();

        if ( lkupSubmissionStep != null ) {
            submissionStepDTO.setId( lkupSubmissionStep.getId() );
            submissionStepDTO.setDescription( lkupSubmissionStep.getDescription() );
        }
        if ( study != null ) {
            submissionStepDTO.setSftpKey( study.getUuid() );
        }
        submissionStepDTO.setValidated( dataSubmission.isValidated() );

        return submissionStepDTO;
    }
}
