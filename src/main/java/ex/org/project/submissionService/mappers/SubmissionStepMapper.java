package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.DataSubmission;
import ex.org.project.submissionService.models.LkupSubmissionStep;
import ex.org.project.submissionService.models.Study;
import ex.org.project.submissionService.models.dtos.SubmissionStepDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SubmissionStepMapper {

    @Mapping(source="lkupSubmissionStep.id",target="id")
    @Mapping(source="lkupSubmissionStep.description",target="description")
    @Mapping(target="validated", expression="java(dataSubmission.isValidated())")
    @Mapping(source="study.uuid",target="sftpKey")
    SubmissionStepDTO toStepDto(LkupSubmissionStep lkupSubmissionStep,DataSubmission dataSubmission, Study study);


}



