package ex.org.project.submissionService.emails;

import ex.org.project.submissionService.auth.AccessRole;
import ex.org.project.submissionService.models.LkupDCC;
import ex.org.project.submissionService.models.Users;
import ex.org.project.submissionService.repositories.LkupDCCRepository;
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
    private final LkupDCCRepository dccRepository;
    private final String supportEmailAddress;
    private final List<String> stakeholderEmailsStudyReg;

    public EmailAddressService(UsersRepository usersRepository,
                               LkupDCCRepository dccRepository,
                               @Value("${supportEmail}") String supportEmailAddress,
                               @Value("${stakeholder-emails-study-reg}") String[] stakeholderEmailsStudyReg){
        this.usersRepository = usersRepository;
        this.dccRepository = dccRepository;
        this.supportEmailAddress = supportEmailAddress;
        this.stakeholderEmailsStudyReg = Arrays.stream(stakeholderEmailsStudyReg).toList();
    }

    /**
     * @param dccName the name of the DCC to find the submitter email addresses for
     * @return list of email addresses for a DCC
     */
    @Transactional(readOnly = true)
    public List<String> getExternalDccEmailAddresses(String dccName) {
        Optional<LkupDCC> dccOpt = dccRepository.findByNameEqualsIgnoreCase(dccName);
        if(dccOpt.isEmpty()) {
            log.error("Could not find DCC with name {}", dccName);
            return new ArrayList<>(0);
        }
        List<Users> submitters = usersRepository.findAllByRoles_NameAndDccAndInternalUserIsFalse(AccessRole.DATA_SUBMITTER.label, dccOpt.get());
        return submitters.stream()
                .map(Users::getEmail)
                .toList();
    }

    /**
     * @param dccName the name of the DCC to find the submitter email addresses for
     * @return list of email addresses for a DCC
     */
    @Transactional(readOnly = true)
    public List<String> getInternalDccEmailAddresses(String dccName) {
        Optional<LkupDCC> dccOpt = dccRepository.findByNameEqualsIgnoreCase(dccName);
        if(dccOpt.isEmpty()) {
            log.error("Could not find DCC with name {}", dccName);
            return new ArrayList<>(0);
        }
        List<Users> submitters = usersRepository.findAllByRoles_NameAndDccAndInternalUserIsTrue(AccessRole.DATA_SUBMITTER.label, dccOpt.get());
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
