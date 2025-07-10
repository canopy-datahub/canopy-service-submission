package ex.org.project.submissionService.exceptions.custom;

public class StatusNotFoundException extends RuntimeException {

    public StatusNotFoundException(String message) {
        super(message);
    }
}