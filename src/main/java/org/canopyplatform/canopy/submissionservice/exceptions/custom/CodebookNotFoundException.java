package org.canopyplatform.canopy.submissionservice.exceptions.custom;

public class CodebookNotFoundException extends RuntimeException{
    public CodebookNotFoundException(String message) {
        super(message);
    }
}
