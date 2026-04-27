package org.canopyplatform.canopy.submissionservice.mappers;

import org.canopyplatform.canopy.submissionservice.models.DataSubmission;
import org.canopyplatform.canopy.submissionservice.models.LkupSubmissionStep;
import org.canopyplatform.canopy.submissionservice.models.Study;
import org.canopyplatform.canopy.submissionservice.models.dtos.SubmissionStepDTO;
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



