package ex.org.project.submissionService.exceptions.custom;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadDataException extends RuntimeException{

    public BadDataException(String message){
        super(message);
    }

}
