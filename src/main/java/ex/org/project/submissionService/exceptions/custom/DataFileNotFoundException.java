package ex.org.project.submissionService.exceptions.custom;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DataFileNotFoundException extends RuntimeException{

    public DataFileNotFoundException(String message) {
        super(message);
    }

}
