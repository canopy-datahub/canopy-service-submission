package ex.org.project.submissionService.emails;

public enum DataIngestEmailType implements EmailRequestType {
    SUBMISSION_CONFIRMATION("Submission Confirmation", "File Submission Confirmation"),
    SUBMISSION_FAILED("Submission Failure", "File Submission Failure"),
    SUBMISSION_PROCESSED("Submission Processed", "Submitted Files Processed"),
    UPLOAD_PORTAL("Upload Portal", "File Upload Received");

    public final String type;
    public final String subject;

    DataIngestEmailType(String type, String subject){
        this.type = type;
        this.subject = subject;
    }

    public String getType(){
        return this.type;
    }

    public String getSubject() {
        return this.subject;
    }
}
