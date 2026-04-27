package org.canopyplatform.canopy.submissionservice.exceptions.custom;

public class StatusNotFoundException extends RuntimeException {

    public StatusNotFoundException(String message) {
        super(message);
    }
}
