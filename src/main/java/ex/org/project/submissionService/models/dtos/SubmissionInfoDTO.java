package ex.org.project.submissionService.models.dtos;

import lombok.Data;

import java.util.Date;

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
