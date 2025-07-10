package ex.org.project.submissionService.models.dtos;

import lombok.Data;

@Data
public class DataFileDTO {

    private Integer id;
    private Integer submissionId;
    private String sourceFileName;
    private String normalizedFileName;
    private Integer versionNumber;
    private Boolean piiPhiPassed;
    private Long fileSize;
    private Boolean validationAcknowledged;
    private Boolean cdeValidationPassed;
    private String fileCategory;
    private String reviewDecision;
}
