package ex.org.project.submissionService.emails;

public enum StudyRegEmailType implements EmailRequestType {
    NEW_STUDY_CREATION("Study Creation", "New Study Created"),
    NEW_STUDY_DCC_METADATA("Study DCC Metadata", "Study Metadata Added"),
    NEW_STUDY_APPROVAL("Study Approval", "Study Added to the Data Hub");

    public final String type;
    public final String subject;

    StudyRegEmailType(String type, String subject){
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
