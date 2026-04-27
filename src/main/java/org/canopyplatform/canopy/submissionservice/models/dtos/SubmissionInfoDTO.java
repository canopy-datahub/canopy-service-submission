package org.canopyplatform.canopy.submissionservice.models.dtos;

import java.util.Date;

import lombok.Data;

@Data
public class SubmissionInfoDTO {
	private Integer id;
	private Integer studyId;
	private String studyName;
	private String status;
	private Date createdDate;
	private Date submittedDate;
	private Date modifiedDate;
	private Date approvedDate;
}
