package ex.org.project.submissionService.exceptions.custom;

public class CodebookNotFoundException extends RuntimeException{
    public CodebookNotFoundException(String message) {
        super(message);
    }
}
