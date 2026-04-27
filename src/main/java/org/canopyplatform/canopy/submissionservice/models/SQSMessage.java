package org.canopyplatform.canopy.submissionservice.models;

import lombok.Data;

@Data
public class SQSMessage {
    private String bucket;
    private String studyUUID;
    private Integer submissionId;
    private ValidationResult validationDTO;

    public SQSMessage(String bucket,String studyUUID, Integer submissionId, ValidationResult validationResults){
        this.bucket = bucket;
        this.studyUUID = studyUUID;
        this.submissionId = submissionId;
        this.validationDTO = validationResults;
    }
}
