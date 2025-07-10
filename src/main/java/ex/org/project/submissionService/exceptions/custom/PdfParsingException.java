package ex.org.project.submissionService.exceptions.custom;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class PdfParsingException extends RuntimeException{

    public PdfParsingException(String message){ super(message); }

}
