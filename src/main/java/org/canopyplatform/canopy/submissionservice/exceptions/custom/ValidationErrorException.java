package org.canopyplatform.canopy.submissionservice.exceptions.custom;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ValidationErrorException extends RuntimeException {
    public ValidationErrorException(String message){
        super(message);
    }
}
