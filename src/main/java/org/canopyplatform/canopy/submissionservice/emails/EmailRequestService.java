package org.canopyplatform.canopy.submissionservice.emails;

import org.canopyplatform.canopy.submissionservice.exceptions.custom.StudyNotFoundException;
import org.canopyplatform.canopy.submissionservice.models.Users;
import org.canopyplatform.canopy.submissionservice.models.ViewStudy;
import org.canopyplatform.canopy.submissionservice.repositories.ViewStudyRepository;
import org.canopyplatform.canopy.submissionservice.services.SqsMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class EmailRequestService {

    private final EmailAddressService emailAddressService;
    private final SqsMessageService messageService;
    private final ViewStudyRepository viewStudyRepository;
    private final String hostname;

    public EmailRequestService(EmailAddressService emailAddressService,
                               SqsMessageService messageService,
                               ViewStudyRepository viewStudyRepository,
                               @Value("${canopy.host-url}") String hostname){
        this.emailAddressService = emailAddressService;
        this.messageService = messageService;
        this.viewStudyRepository = viewStudyRepository;
        this.hostname = hostname;
    }

    /**
     * Sends an email to stakeholders involved in the study registration process
     * @param studyId the ID of the study that is the subject of the email
     * @param requestType the type of email to be sent
     */
    public void sendStudyRegEmail(Integer studyId, StudyRegEmailType requestType) throws StudyNotFoundException {
        ViewStudy study = getViewStudy(studyId);
        String supportEmailAddress = emailAddressService.getSupportEmailAddress();
        List<String> to = new ArrayList<>();
        List<String> cc = new ArrayList<>();
        cc.add(supportEmailAddress);
        Map<String, String> props = new HashMap<>();
        props.put("studyId", String.valueOf(study.getStudyId()));
        props.put("studyName", study.getStudyName());

        switch(requestType) {
            case NEW_STUDY_CREATION:
                to.addAll(emailAddressService.getExternalCenterEmailAddresses(study.getCenter()));
                cc.addAll(emailAddressService.getInternalCenterEmailAddresses(study.getCenter()));
                cc.addAll(emailAddressService.getCuratorEmailAddresses());
                cc.addAll(emailAddressService.getAdditionalStakeholderEmailsStudyReg());
                break;
            case NEW_STUDY_APPROVAL:
                props.put("studyLink", String.format("%s/study/%d", hostname, study.getStudyId()));
                to.addAll(emailAddressService.getExternalCenterEmailAddresses(study.getCenter()));
                cc.addAll(emailAddressService.getInternalCenterEmailAddresses(study.getCenter()));
                cc.addAll(emailAddressService.getCuratorEmailAddresses());
                cc.addAll(emailAddressService.getAdditionalStakeholderEmailsStudyReg());
                break;
        }

        EmailRequest request = new EmailRequest(
                requestType,
                to,
                cc,
                supportEmailAddress,
                props
        );
        messageService.sendEmail(request);
    }

    public void sendSftpEmail(SftpEmailType requestType, Users user, Map<String, String> additionalProps) throws StudyNotFoundException {
        String supportEmailAddress = emailAddressService.getSupportEmailAddress();
        List<String> to = new ArrayList<>();
        to.add(user.getEmail());
        List<String> cc = new ArrayList<>();
        cc.add(supportEmailAddress);
        cc.addAll(emailAddressService.getCuratorEmailAddresses());
        Map<String, String> props = new HashMap<>();
        props.put("dashboardLink", String.format("%s/submitterDashboard", hostname));
        props.putAll(additionalProps);
        EmailRequest request = new EmailRequest(
                requestType,
                to,
                cc,
                supportEmailAddress,
                props
        );
        messageService.sendEmail(request);
    }

    /**
     * Sends an email to stakeholders involved in the data ingest process
     * @param submissionId the ID of the submission that is the subject of the email
     * @param requestType the type of email to be sent
     */
    public void sendDataIngestEmail(Integer submissionId, DataIngestEmailType requestType) throws StudyNotFoundException {
        sendDataIngestEmail(submissionId, requestType, new HashMap<>(0));
    }

    /**
     * Sends an email to stakeholders involved in the data ingest process
     * @param submissionId the ID of the submission that is the subject of the email
     * @param requestType the type of email to be sent
     * @param additionalProps any additional necessary parameters, especially as it pertains to approved/rejected files
     */
    public void sendDataIngestEmail(Integer submissionId, DataIngestEmailType requestType, Map<String, String> additionalProps) throws StudyNotFoundException {
        ViewStudy study = getViewStudyBySubmissionId(submissionId);
        String supportEmailAddress = emailAddressService.getSupportEmailAddress();
        List<String> to = emailAddressService.getSubmitterEmailAddresses(submissionId);
        List<String> cc = new ArrayList<>();
        cc.add(supportEmailAddress);
        cc.addAll(emailAddressService.getCuratorEmailAddresses());
        Map<String, String> props = new HashMap<>();
        props.put("studyId", String.valueOf(study.getStudyId()));
        props.put("studyName", study.getStudyName());
        props.putAll(additionalProps);

        EmailRequest request = new EmailRequest(
                requestType,
                to,
                cc,
                supportEmailAddress,
                props
        );
        messageService.sendEmail(request);
    }

    private ViewStudy getViewStudy(Integer studyId){
        try {
            return viewStudyRepository.findByStudyId(studyId).orElseThrow();
        }
        catch(NoSuchElementException e) {
            String errorMessage = String.format("Could not find study ID %d", studyId);
            log.error(errorMessage);
            throw new StudyNotFoundException(errorMessage);
        }
    }

    private ViewStudy getViewStudyBySubmissionId(Integer submissionId){
        try {
            return viewStudyRepository.findBySubmissionId(submissionId).orElseThrow();
        }
        catch(NoSuchElementException e) {
            String errorMessage = String.format("Could not find study for submission ID %d", submissionId);
            log.error(errorMessage);
            throw new StudyNotFoundException(errorMessage);
        }
    }

    public void sendUploadPortalEmail(String fileName, String studyName, Users submitter){
        String supportEmailAddress = emailAddressService.getSupportEmailAddress();
        List<String> to = List.of(submitter.getEmail());
        List<String> cc = new ArrayList<>();
        cc.addAll(emailAddressService.getCuratorEmailAddresses());
        cc.add(supportEmailAddress);
        Map<String, String> props = new HashMap<>();
        props.put("fileName", fileName);
        props.put("studyName", studyName);
        props.put("userName", submitter.getFullName());
        EmailRequest request = new EmailRequest(
                DataIngestEmailType.UPLOAD_PORTAL,
                to,
                cc,
                supportEmailAddress,
                props
        );
        messageService.sendEmail(request);
    }

}
