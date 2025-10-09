package ex.org.project.submissionService.services;

import ex.org.project.submissionService.auth.UserAuthorizationException;
import ex.org.project.submissionService.auth.UserNotFoundException;
import ex.org.project.submissionService.emails.EmailRequestService;
import ex.org.project.submissionService.emails.StudyRegEmailType;
import ex.org.project.submissionService.exceptions.custom.*;
import ex.org.project.submissionService.mappers.StudyPropertyValueMapper;
import ex.org.project.submissionService.mappers.ViewStudyCenterMapper;
import ex.org.project.submissionService.models.*;
import ex.org.project.submissionService.models.dtos.StudyPropertyValueDTO;
import ex.org.project.submissionService.models.dtos.StudyRegistrationDTO;
import ex.org.project.submissionService.models.dtos.UserStudyRegistrationDTO;
import ex.org.project.submissionService.repositories.*;

import ex.org.project.submissionService.utils.LambdaUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.lambda.LambdaClient;


@Slf4j
@Service
@RequiredArgsConstructor
public class StudyRegistrationService {
    private final LkupCenterRepository centerRepository;
    private final StudyRepository studyRepository;
    private final EntityPropertyRepository entityPropertyRepository;
    private final StudyPropertyValueRepository studyPropertyValueRepository;
    private final LkupPropertyCodelistValueRepository codelistValueRepository;
    private final EntityPropertyMtaMappingRepository mtaMappingRepository;
    private final LkupPropertySourceRepository propertySourceRepository;
    private final StudyPropertyValueMapper studyPropertyValueMapper;
    private final LkupStatusRepository statusRepository;
    private final UsersRepository usersRepository;
    private final ViewStudyRepository viewStudyRepository;
    private final ViewStudyCenterMapper viewStudyCenterMapper;
    private final AwsStorageService awsStorageService;
    private final EmailRequestService emailRequestService;
    private final UserFileUploadRepository uploadRepository;
    private final LambdaClient awsLambdaClient;

    private final String CURATOR = "Curator";
    private final String DATA_SUBMITTER = "Center";
    private final Pattern valueIndexMatcher = Pattern.compile("(\\d+)");
    private static final List<String> PROPERTY_SOURCES = List.of("dbGaP/MTA", "Online Submission");

    @Value("${radx.opensearch-lambda}") String openSearchLambda;

    /**
     * Register a new study based on the study registration form
     *
     * @param studyRegistrationDTO DTO containing any Study Property Value curator added during study registration
     * @return the id of the newly created study
     */
    @Transactional
    public Map<String, Integer> registerNewStudy(StudyRegistrationDTO studyRegistrationDTO, String role, Integer userId, Boolean shouldSubmit) {
        Study study = createStudy(studyRegistrationDTO, userId);

        //have a new StudyRegistrationDTO instance that has the study id
        Integer studyId = study.getId();
        StudyRegistrationDTO studyRegistrationDTOWithId = new StudyRegistrationDTO(studyId, studyRegistrationDTO.studyPropertyValues());

        updateStudyPropertyValues(studyRegistrationDTOWithId, role, userId, shouldSubmit, true);

        emailRequestService.sendStudyRegEmail(studyId, StudyRegEmailType.NEW_STUDY_CREATION);
        return Map.of("studyId", studyId);
    }

    /**
     * Returns all study property values for a specific study
     * @param studyId ID of the study being returned
     * @return DTO containing all study property values associated with the supplied study ID
     */
    public StudyRegistrationDTO getStudyProperties(Integer studyId) {
        //check if study id is valid
        studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyNotFoundException("Study not found for study ID " + studyId));
        List<StudyPropertyValue> studyPropertyValueList = studyPropertyValueRepository.findAllByStudyId(studyId);
        if (studyPropertyValueList.isEmpty()) {
            throw new StudyNotFoundException("No property values found for study: " + studyId);
        }
        List<StudyPropertyValueDTO> spvDtoList = studyPropertyValueMapper.entityListToDtoList(studyPropertyValueList);
        return new StudyRegistrationDTO(studyId, spvDtoList);
    }

    /**
     * Creates a new record on the study table
     *
     * @return ID of the newly created study
     */
    private Study createStudy(StudyRegistrationDTO studyRegistrationDTO, Integer userId) {
        Study study = new Study();
        study.setUuid(UUID.randomUUID().toString());
        LkupStatus status = statusRepository.findByUsageAndName(Constants.USAGE_STUDY, Constants.STATUS_DRAFT_STUDY)
            .orElseThrow(()  -> new StatusNotFoundException(String.format("Invalid Study Status: %s", Constants.STATUS_DRAFT_STUDY)));
        study.setStatus(status);
        study.setCreatedAt(Timestamp.from(Instant.now()));
        study.setCreatedBy(userId);
        //todo: need to change
        Optional<LkupCenter> dccOpt = centerRepository.findByNameContainingIgnoreCase("RADx-UP");
        LkupCenter dcc = dccOpt.get();
        study.setCenter(dcc);
        study = studyRepository.saveAndFlush(study);
        log.info("New study created: {}", study);
        return study;
    }


    private Map<Integer, Set<String>> getCodelistValues(List<LkupPropertyCodelistValue> codelistValuesList) {
        return codelistValuesList.stream()
                .collect(Collectors.groupingBy(
                        LkupPropertyCodelistValue::getPropertyCodelistId,
                        Collectors.mapping(LkupPropertyCodelistValue::getValue, Collectors.toSet()))
                );
    }

    /**
     * Allows a curator to edit any existing Study Property Value or add additional entries
     *
     * @param studyRegistrationDTO DTO containing any Study Property Value additions or modifications
     * @return DTO containing all study property values associated with the supplied study ID
     */
    @Transactional
    public String editStudyPropertyValues(StudyRegistrationDTO studyRegistrationDTO, String role, Boolean shouldSubmit, Integer userId) {
        Optional<Study> studyOpt = studyRepository.findById(studyRegistrationDTO.studyId());
        if (studyOpt.isEmpty()) {
            throw new StudyNotFoundException("No study found with ID: " + studyRegistrationDTO.studyId());
        }

        updateStudyPropertyValues(studyRegistrationDTO, role, userId, shouldSubmit, false);

        return "Successfully updated property values";
    }

    public String updateStudyPropertyValues(StudyRegistrationDTO studyRegistrationDTO, String role, Integer userId, Boolean shouldSubmit, Boolean isNewStudy) {
        Optional<Study> studyOpt = studyRepository.findById(studyRegistrationDTO.studyId());
        if (studyOpt.isEmpty()) {
            throw new StudyNotFoundException("No study found with ID: " + studyRegistrationDTO.studyId());
        }

        List<LkupPropertySource> propertySources = propertySourceRepository.findAllByNameIn(PROPERTY_SOURCES);

        List<Integer> sourceIds = propertySources.stream()
            .map(LkupPropertySource::getId)
            .toList();
        List<EntityProperty> studyRegistrationEntityProperties = entityPropertyRepository
            .findAllByPropertySourceIdIn(sourceIds);
        Set<Integer> epIds = studyRegistrationEntityProperties.stream()
            .map(EntityProperty::getId)
            .collect(Collectors.toSet());

        // studyRegistrationDTO should only contain entries for spv's that are being modified or added
        List<StudyPropertyValue> propertyValues = studyPropertyValueMapper
            .dtoListToEntityList(studyRegistrationDTO.studyPropertyValues())
            .stream()
            .filter(spv -> spv.getEntityProperty()== null || epIds.contains(spv.getEntityProperty().getId()))
            .toList();
        if(propertyValues.size() != studyRegistrationDTO.studyPropertyValues().size()){
            throw new UserAuthorizationException("User is attempting edits to unauthorized properties");
        }

        // For new registered study, need to manually add the property "has_data_files"
        if(isNewStudy){
            Integer studyId = studyRegistrationDTO.studyId();
            StudyPropertyValue hasDataFilesPropertyValue = createPropertyValue(studyId, userId, "has_data_files", "No");
            studyPropertyValueRepository.save(hasDataFilesPropertyValue);
        }

        //true for property values to delete, false for property values to edit
        Map<Boolean, List<StudyPropertyValue>> shouldBeRemoved = propertyValues.stream()
            .collect(Collectors.partitioningBy(StudyPropertyValue::getShouldBeRemoved));

        checkForNullEntityProperties(shouldBeRemoved.get(false));
        deleteStudyValueProperties(shouldBeRemoved.get(true));
        List<LkupPropertyCodelistValue> codelistValuesList = codelistValueRepository.findAll();

        for (StudyPropertyValue spv : shouldBeRemoved.get(false)) {
            System.out.println(spv.getEntityProperty().getName());
            Optional<EntityProperty> spvEntityPropertyOpt = studyRegistrationEntityProperties.stream()
                .filter(ep -> Objects.equals(ep.getId(), spv.getEntityProperty().getId()))
                .findFirst();
            if (spvEntityPropertyOpt.isEmpty()) {
                throw new CategoryNotFoundException("No entity property found for study property value: " + spv);
            }
            EntityProperty ep = spvEntityPropertyOpt.get();
            spv.setPropertyValue(spv.getPropertyValue().trim());
            if (ep.getCodeListId() == null) {
                editOneToOnePropertyValue(spv, ep, studyRegistrationDTO.studyId(), userId);
            } else {
                editCodelistedPropertyValue(spv, ep, studyRegistrationDTO.studyId(), codelistValuesList, userId);
            }
        }

        updateStatus(studyOpt.get(), role, userId, shouldSubmit, isNewStudy);

        return "Successfully updated property values";
    }

    private void checkForNullEntityProperties(List<StudyPropertyValue> spvList) {
        boolean hasEmptyEP = spvList.stream().anyMatch(spv -> Objects.isNull(spv.getEntityProperty()));
        if(hasEmptyEP) {
            throw new StudyRegRequestException("New Study Properties must have an Entity Property");
        }
    }

    /**
     * Deletes study property values from the database by id
     *
     * @param studyPropertyValueList list of property values that were marked to be removed
     */
    private void deleteStudyValueProperties(List<StudyPropertyValue> studyPropertyValueList) {
        studyPropertyValueRepository.deleteAllById(
                studyPropertyValueList.stream()
                        .map(StudyPropertyValue::getId)
                        .filter(Objects::nonNull)
                        .toList()
        );
    }

    /**
     * Changes the submission status of a study based on submissions by specific roles
     *
     * @param study study being updated
     * @param role  the user role of the submission step
     * @param userId id of user updating status
     * @param shouldSubmit  true if click submit
     * @param isNewStudy true if it is a new registered study
     */
    private void updateStatus(Study study, String role, Integer userId, Boolean shouldSubmit, Boolean isNewStudy) {
        switch (role) {
            case DATA_SUBMITTER -> {
                if (shouldSubmit) {
                    setStudyStatus(study, Constants.STATUS_IN_REVIEW);
                     emailRequestService.sendStudyRegEmail(study.getId(), StudyRegEmailType.NEW_STUDY_DCC_METADATA);
                } else if (isNewStudy) {
                    setStudyStatus(study, Constants.STATUS_DRAFT_STUDY);
                }
            }
            case CURATOR -> {
                if(shouldSubmit){
                    setStudyStatus(study, Constants.STATUS_APPROVED_STUDY);
                    updateReleaseDate(study, userId);
                     emailRequestService.sendStudyRegEmail(study.getId(), StudyRegEmailType.NEW_STUDY_APPROVAL);
                } else if (isNewStudy) { //Save the update
                    setStudyStatus(study, Constants.STATUS_DRAFT_STUDY);
                }
            }
            default -> throw new BadDataException("Invalid role when updating study status");
        }
    }

    private void setStudyStatus(Study study, String statusName) {
        LkupStatus status = statusRepository.findByUsageAndName(Constants.USAGE_STUDY, statusName)
            .orElseThrow(() -> new StatusNotFoundException(String.format("Could not find study status entity. Invalid Study Status: %s", statusName)));
        study.setStatus(status);
        studyRepository.saveAndFlush(study);
    }

    private void updateReleaseDate(Study study, Integer userId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        StudyPropertyValue releaseDate = createPropertyValue(study.getId(), userId, "release_date", formatter.format(LocalDate.now()));
        studyPropertyValueRepository.save(releaseDate);
    }

    /**
     * Updates existing one to one property values, or creates new entries in the database
     *
     * @param spv     property value to be updated or added in the database
     * @param ep      corresponding entity property
     * @param studyId associated study
     * @param userId id of user updating status
     */
    private void editOneToOnePropertyValue(StudyPropertyValue spv, EntityProperty ep, Integer studyId,  Integer userId) {
        if (spv.getId() != null) {
            Optional<StudyPropertyValue> spvOpt = studyPropertyValueRepository.findById(spv.getId());
            if (spvOpt.isEmpty()) {
                throw new StudyPropertyValuesRetrievalException("Could not find existing property value with ID: " + spv.getId(), new Throwable());
            }
            StudyPropertyValue spvToUpdate = spvOpt.get();
            spvToUpdate.setModifiedAt(Timestamp.from(Instant.now()));
            spvToUpdate.setModifiedBy(userId);
            spvToUpdate.setPropertyValue(spv.getPropertyValue());
            studyPropertyValueRepository.save(spvToUpdate);
        } else {
            StudyPropertyValue studyPropertyValue = new StudyPropertyValue();
            studyPropertyValue.setStudyId(studyId);
            studyPropertyValue.setPropertyValue(spv.getPropertyValue());
            studyPropertyValue.setEntityProperty(ep);
            studyPropertyValue.setCreatedBy(userId);
            studyPropertyValueRepository.save(studyPropertyValue);
        }
    }

    /**
     * Updates existing codelisted property values, or creates new entries in the database
     *
     * @param spv     property value to be updated or added in the database
     * @param ep      corresponding entity property
     * @param studyId associated study
     * @param userId id of user updating status
     */
    private void editCodelistedPropertyValue(StudyPropertyValue spv, EntityProperty ep, Integer studyId, List<LkupPropertyCodelistValue> codelistValuesList,  Integer userId) {
        Map<Integer, Set<String>> codelistValues = getCodelistValues(codelistValuesList);
        Set<String> codelistValuesSet = codelistValues.get(ep.getCodeListId());
        Optional<String> clValue = codelistValuesSet.stream()
                .filter(spv.getPropertyValue()::equalsIgnoreCase)
                .findAny();
        if (clValue.isPresent()) {
            spv.setPropertyValue(clValue.get());
        } else {
            throw new BadDataException("Provided property value is not a codelisted value.");
        }

        editOneToOnePropertyValue(spv, ep, studyId, userId);
    }

    /**
     * Retrieves a list of UserStudyRegistrationDTO objects based on the DCC to which a user is aligned
     *
     * @param userId ID of the user in the db
     * @return A list of UserStudyRegistrationDTO objects.
     * @throws UserAuthorizationException If the user is not aligned to a DCC
     */
    @Transactional(readOnly = true)
    public List<UserStudyRegistrationDTO> getUserStudiesByCenter(Integer userId, String status) {
        Users users = usersRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("No user found for user ID " + userId));
        if(users.getCenter() == null || users.getCenter().getId() == null){
            throw new UserAuthorizationException("User is not aligned to a DCC");
        }

        // Find dcc studies by status, ordering by status, creation date
        //check if status is valid
        statusRepository.findByName(status)
                .orElseThrow(()  -> new StatusNotFoundException(String.format("Invalid Study Status: %s", status)));
        List<ViewStudy> viewStudies = viewStudyRepository.findCenterStudiesByStatus(users.getCenter().getId(), status);
        return viewStudyCenterMapper.toDTOs(viewStudies);
    }

    /**
     * Retrieve a list of UserStudyRegistrationDTO objects for the current curator.
     *
     * @return A list of UserStudyRegistrationDTO objects.
     */
    @Transactional(readOnly = true)
    public List<UserStudyRegistrationDTO> getUserStudiesByCurator(String status) {
        statusRepository.findByName(status)
                .orElseThrow(()  -> new StatusNotFoundException(String.format("Invalid Study Status: %s", status)));
        List<ViewStudy> studies = viewStudyRepository.findCuratorStudiesByStatus(status);
        return viewStudyCenterMapper.toDTOs(studies);
    }

    /**
     * Deletes a study and its associated properties by the curator.
     *
     * @param studyId The ID of the study to be deleted.
     */
    @Transactional
    public void deleteStudiesByCurator(Integer studyId) {
        // Find the study by its ID
        //Allow approved studies to be deleted
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new StudyNotFoundException(
                        String.format("Study ID %d not found", studyId)));


        if (study.getFileUrl() == null) {
            log.error(String.format("File URL is null for study ID %d, UUID %s", studyId, study.getUuid()));
        } else {
            List<UserFileUpload> userFileUploads = uploadRepository.findAllByStudyId(studyId);
            uploadRepository.deleteAll(userFileUploads);
            log.info("Deleted user_file_upload records for study id: {}: \n {} ", studyId, StringUtils.join(userFileUploads, "/n"));
            boolean deletionSuccessful = awsStorageService.deleteStudyFromS3(study);
            if(!deletionSuccessful) {
                throw new FileDeletionException(String.format("Could not delete Study ID %d", studyId));
            }
        }
        studyPropertyValueRepository.deleteAllByStudyId(studyId);
        studyRepository.delete(study);
    }

    private StudyPropertyValue createPropertyValue(Integer studyId, Integer userId, String name, String value) {
        StudyPropertyValue studyPropertyValue = new StudyPropertyValue();
        studyPropertyValue.setStudyId(studyId);
        studyPropertyValue.setPropertyValue(value);
        studyPropertyValue.setCreatedBy(userId);
        studyPropertyValue.setShouldBeRemoved(false);
        Optional<EntityProperty> ep = entityPropertyRepository.findByName(name);
        if (ep.isEmpty()) {
            throw new CategoryNotFoundException("Could not find entity property");
        }
        studyPropertyValue.setEntityProperty(ep.get());
        return studyPropertyValue;
    }

    /**
     * This function invokes lambda function to refresh openSearch index
     */
    public void triggerOpenSearchRefresh(){
        LambdaUtils.invokeFunction(awsLambdaClient,openSearchLambda);
    }

}
