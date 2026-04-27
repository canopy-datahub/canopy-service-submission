package org.canopyplatform.canopy.submissionservice.exceptions.custom;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EmptyParameterException extends RuntimeException{

    public EmptyParameterException(String message){
        super(message);
    }

}
