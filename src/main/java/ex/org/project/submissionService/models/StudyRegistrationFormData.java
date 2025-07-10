package ex.org.project.submissionService.models;

import lombok.Data;

@Data
public class StudyRegistrationFormData {

    private Boolean institutionalCerts; //Institutional Certifications
    private Boolean dataSharingSubmissionInfo; //NHGRI Genomic Data Sharing  Submission Information
    private String studyName; //Study name
    private Boolean multisiteStudy; //Multisite radio
    private String multicenterList; //Multicenter list
    private Boolean dataSubmission; //Data submission Radio?
}
