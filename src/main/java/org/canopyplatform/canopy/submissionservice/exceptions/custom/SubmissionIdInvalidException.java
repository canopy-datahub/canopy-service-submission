package org.canopyplatform.canopy.submissionservice.exceptions.custom;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SubmissionIdInvalidException extends RuntimeException {
    public SubmissionIdInvalidException(String message) {
        super(message);
    }
}
