package org.canopyplatform.canopy.submissionservice.emails;

public enum SftpEmailType implements EmailRequestType{

    SFTP_PROCESSED("SFTP Processed", "SFTP Upload Processed");

    public final String type;
    public final String subject;

    SftpEmailType(String type, String subject){
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
