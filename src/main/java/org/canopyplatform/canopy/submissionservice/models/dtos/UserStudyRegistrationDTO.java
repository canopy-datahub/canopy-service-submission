package org.canopyplatform.canopy.submissionservice.models.dtos;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class UserStudyRegistrationDTO {

    private Integer studyId;
    private String studyName;
    private String status;
    private Timestamp createdAt;
    private Boolean hasDataFiles;

}
