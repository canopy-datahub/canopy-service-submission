package org.canopyplatform.canopy.submissionservice.mappers;

import java.util.List;

import org.canopyplatform.canopy.submissionservice.models.ViewStudy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.canopyplatform.canopy.submissionservice.models.DataSubmission;
import org.canopyplatform.canopy.submissionservice.models.dtos.SubmissionInfoDTO;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface SubmitterInfoMapper {

	@Mapping(source = "dataSubmission.id", target = "id")
	@Mapping(source = "dataSubmission.studyId", target = "studyId")
	@Mapping(source = "dataSubmission.study", target = "studyName", qualifiedByName = "prependPhs")
	@Mapping(source = "dataSubmission.status.name", target = "status")
	@Mapping(source = "dataSubmission.createdAt", target = "createdDate")
	@Mapping(source = "dataSubmission.dateSubmitted", target = "submittedDate")
	@Mapping(source = "dataSubmission.modifiedAt", target = "modifiedDate")
	@Mapping(source = "dataSubmission.dateApproved", target = "approvedDate")
	SubmissionInfoDTO toDTO(DataSubmission dataSubmission);

	List<SubmissionInfoDTO> toDTOList(List<DataSubmission> dataSubmissions);

	@Named("prependPhs")
	default String prependPhs(ViewStudy study) {
		return String.format("(%s) %s", study.getStudyId(), study.getStudyName());
	}
}
