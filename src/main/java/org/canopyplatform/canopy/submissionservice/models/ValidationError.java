package org.canopyplatform.canopy.submissionservice.models;

import lombok.Data;


@Data
public class ValidationError {
    private String validationType;
    String errorType;
    String value;
    String columnHeader;
    Long lineNumber;
    String message;
    String solution;

    public ValidationError() {
        super();
    }
    public ValidationError(String validationType, String value, String errorType, String columnHeader, Long lineNumber, String message, String solution){
        this.validationType = validationType;
        this.errorType = errorType;
        this.message = message;
        this.columnHeader = columnHeader;
        this.lineNumber = lineNumber;
        this.solution = solution;
        this.value = value;
    }

}
