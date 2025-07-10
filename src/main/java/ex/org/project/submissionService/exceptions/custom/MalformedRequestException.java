package ex.org.project.submissionService.exceptions.custom;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MalformedRequestException extends RuntimeException{
    public MalformedRequestException(String message) { super(message); }
}