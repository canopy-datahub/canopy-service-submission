package ex.org.project.submissionService.models.dtos;

import java.sql.Timestamp;

import lombok.Data;

@Data
public class DataSubmissionDTO {
	private Integer id;
	private Integer studyId;
	private String studyName;
	private String phs;
	private String DCC;
	private Timestamp submissionDate;
	private Timestamp createdAt;
	private String DCCRepresentative;
	private String submissionStatus;
}
