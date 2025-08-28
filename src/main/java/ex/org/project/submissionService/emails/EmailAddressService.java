package ex.org.project.submissionService.emails;

import ex.org.project.submissionService.auth.AccessRole;
import ex.org.project.submissionService.models.LkupCenter;
import ex.org.project.submissionService.models.Users;
import ex.org.project.submissionService.repositories.LkupCenterRepository;
import ex.org.project.submissionService.repositories.UsersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Class to handle email address lookups and storage
 */
@Slf4j
@Service
public class EmailAddressService {

    private final UsersRepository usersRepository;
    private final LkupCenterRepository centerRepository;
    private final String supportEmailAddress;
    private final List<String> stakeholderEmailsStudyReg;

    public EmailAddressService(UsersRepository usersRepository,
                               LkupCenterRepository centerRepository,
                               @Value("${supportEmail}") String supportEmailAddress,
                               @Value("${stakeholder-emails-study-reg}") String[] stakeholderEmailsStudyReg){
        this.usersRepository = usersRepository;
        this.centerRepository = centerRepository;
        this.supportEmailAddress = supportEmailAddress;
        this.stakeholderEmailsStudyReg = Arrays.stream(stakeholderEmailsStudyReg).toList();
    }

    /**
     * @param centerName the name of the center to find the submitter email addresses for
     * @return list of email addresses for a DCC
     */
    @Transactional(readOnly = true)
    public List<String> getExternalCenterEmailAddresses(String centerName) {
        Optional<LkupCenter> centerOpt = centerRepository.findByNameEqualsIgnoreCase(centerName);
        if(centerOpt.isEmpty()) {
            log.error("Could not find DCC with name {}", centerName);
            return new ArrayList<>(0);
        }
        List<Users> submitters = usersRepository.findAllByRoles_NameAndCenterAndInternalUserIsFalse(AccessRole.DATA_SUBMITTER.label, centerOpt.get());
        return submitters.stream()
                .map(Users::getEmail)
                .toList();
    }

    /**
     * @param centerName the name of the center to find the submitter email addresses for
     * @return list of email addresses for a center
     */
    @Transactional(readOnly = true)
    public List<String> getInternalCenterEmailAddresses(String centerName) {
        Optional<LkupCenter> centerOpt = centerRepository.findByNameEqualsIgnoreCase(centerName);
        if(centerOpt.isEmpty()) {
            log.error("Could not find center with name {}", centerName);
            return new ArrayList<>(0);
        }
        List<Users> submitters = usersRepository.findAllByRoles_NameAndCenterAndInternalUserIsTrue(AccessRole.DATA_SUBMITTER.label, centerOpt.get());
        return submitters.stream()
                .map(Users::getEmail)
                .toList();
    }


    /**
     * Retrieves the email addresses of all users with the curator role
     * @return list of email addresses
     */
    @Transactional(readOnly = true)
    public List<String> getCuratorEmailAddresses(){
        List<Users> curators = usersRepository.findAllByRoles_Name(AccessRole.DATA_CURATOR.label);
        return curators.stream()
                .map(Users::getEmail)
                .toList();
    }

    /**
     * Retrieves the email addresses of any additional stakeholders involved in study registration
     * e.g. NIH officers
     * @return list of email addresses
     */
    public List<String> getAdditionalStakeholderEmailsStudyReg(){
        return stakeholderEmailsStudyReg;
    }

    /**
     * Retrieves the email address being used for the Data Hub system automated emails
     * @return email address
     */
    public String getSupportEmailAddress(){
        return this.supportEmailAddress;
    }

    /**
     * Retrieves the email address of the user who initiated a file submission
     * @param submissionId the ID of submission being queried
     * @return list of email addresses (currently will always return 1 email address)
     */
    @Transactional(readOnly = true)
    public List<String> getSubmitterEmailAddresses(Integer submissionId){
        try {
            Users user = usersRepository.findBySubmissionId(submissionId).orElseThrow();
            return List.of(user.getEmail());
        }
        catch(NoSuchElementException e) {
            log.error(String.format("Could not find user for submission ID %d", submissionId));
            return List.of(this.supportEmailAddress);
        }
    }
}
