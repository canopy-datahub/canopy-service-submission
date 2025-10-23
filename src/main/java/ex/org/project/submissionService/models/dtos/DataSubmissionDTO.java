package ex.org.project.submissionService.models.dtos;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class DataSubmissionDTO {
	private Integer id;
	private Integer studyId;
	private String studyName;
	private String phs;
	private String center;
	private Timestamp submissionDate;
	private Timestamp createdAt;
	private String centerRepresentative;
	private String submissionStatus;
}
