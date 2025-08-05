package ex.org.project.submissionService.services;

import ex.org.project.submissionService.auth.UserAuthorizationException;
import ex.org.project.submissionService.auth.UserNotFoundException;
import ex.org.project.submissionService.emails.EmailRequestService;
import ex.org.project.submissionService.exceptions.custom.*;
import ex.org.project.submissionService.mappers.StudyPropertyValueMapper;
import ex.org.project.submissionService.mappers.ViewStudyDccMapper;
import ex.org.project.submissionService.models.*;
import ex.org.project.submissionService.models.dtos.StudyPropertyValueDTO;
import ex.org.project.submissionService.models.dtos.StudyRegistrationDTO;
import ex.org.project.submissionService.models.dtos.UserStudyRegistrationDTO;
import ex.org.project.submissionService.emails.StudyRegEmailType;
import ex.org.project.submissionService.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@Service
@RequiredArgsConstructor
public class StudyRegistrationService {
    private final PdfService pdfService;
    private final StorageService storageService;
    private final LkupDCCRepository dccRepository;
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
    private final ViewStudyDccMapper viewStudyDccMapper;
    private final AwsStorageService awsStorageService;
    private final EmailRequestService emailRequestService;
    private final UserFileUploadRepository uploadRepository;

    private final Pattern valueIndexMatcher = Pattern.compile("(\\d+)");
    private final Pattern fileNameMatcher = Pattern.compile("^([^_]+)_phs(\\d+)_([^_]+).*.pdf");
    private static final Integer PHS_DIGIT_LENGTH = 6;
    private static final List<String> CURATOR_PROPERTY_SOURCES = List.of("dbGaP/MTA", " Hub Online Submission");
    private static final List<String> DCC_PROPERTY_SOURCES = List.of(" Hub Online Submission");

    /**
     * Register a new study based on the study registration form
     *
     * @param studyRegistrationDTO DTO containing any Study Property Value curator added during study registration
     * @return the id of the newly created study
     */
    @Transactional
    public Map<String, Integer> registerNewStudy(StudyRegistrationDTO studyRegistrationDTO, Integer userId) {
        Study study = createStudy(studyRegistrationDTO, userId);

        //have a new StudyRegistrationDTO instance that has the study id
        Integer studyId = study.getId();
        StudyRegistrationDTO studyRegistrationDTOWithId = new StudyRegistrationDTO(studyId, studyRegistrationDTO.studyPropertyValues());

        editStudyPropertyValues(studyRegistrationDTOWithId, "Curator", true, userId);

//        emailRequestService.sendStudyRegEmail(studyId, StudyRegEmailType.NEW_STUDY_CREATION);
        return Map.of("studyId", studyId);
    }

    /**
     * Returns all study property values for a specific study
     *
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
        LkupStatus status = statusRepository.findByUsageAndName(Constants.USAGE_STUDY, Constants.STATUS_PENDING_DCC_INPUT)
            .orElseThrow(()  -> new StatusNotFoundException(String.format("Invalid Study Status: %s", Constants.STATUS_PENDING_DCC_INPUT)));
        study.setStatus(status);
        study.setCreatedAt(Timestamp.from(Instant.now()));
        study.setCreatedBy(userId);
        study = studyRepository.save(study);
        log.info("New study created: {}", study);
        return study;
    }

    /**
     * Parses a provided file name for a dcc, phs number, and study title
     *
     * @param filename filename to be parsed
     * @return map of fields parsed to their values
     */
    private Map<String, String> parseFileName(String filename) {
        Map<String, String> filenameMatchesMap = new HashMap<>(3);
        Matcher matcher = fileNameMatcher.matcher(filename);
        if (matcher.matches()) {
            filenameMatchesMap.put("dcc", matcher.group(1));
            filenameMatchesMap.put("phs", matcher.group(2));
            filenameMatchesMap.put("title", matcher.group(3));
        } else {
            log.error("No filename matches found for filename: {}", filename);
            throw new PdfParsingException("Please check the format of the file name");
        }
        return filenameMatchesMap;
    }

    /**
     * Creates a study property value for a phs that was parsed from a filename
     *
     * @param partialPhsNumber only the digits from a phs number, with or without padded '0' chars
     * @param studyId          which study this will be saved to
     * @return the newly created PHS number study value property
     */
    private StudyPropertyValue createPhsNumberPropertyValue(String partialPhsNumber, Integer studyId, Integer userId) {
        int zeroPadding = PHS_DIGIT_LENGTH - partialPhsNumber.length();
        StringBuilder stringBuilder = new StringBuilder(partialPhsNumber);
        int i = 0;
        while (i < zeroPadding) {
            stringBuilder.insert(0, "0");
            i++;
        }
        stringBuilder.insert(0, "phs");
        String fullPhs = stringBuilder.toString();

        StudyPropertyValue studyPropertyValue = new StudyPropertyValue();
        studyPropertyValue.setStudyId(studyId);
        studyPropertyValue.setPropertyValue(fullPhs);
        studyPropertyValue.setCreatedBy(userId);
        Optional<EntityProperty> ep = entityPropertyRepository.findByName("phs");
        if (ep.isEmpty()) {
            throw new CategoryNotFoundException("Could not find PHS entity property");
        }
        studyPropertyValue.setEntityProperty(ep.get());
        return studyPropertyValue;
    }

    /**
     * Maps all the data from the MTA form PDF to Study Property Value entities
     *
     * @param studyId      ID of the study that is being processed
     * @param parsedPdfMap Object containing all the data that was parsed from the MTA form
     * @return entities from the form to be saved to the database
     */
    private List<StudyPropertyValue> mapPdfFieldsToStudyPropertyValues(Integer studyId, Map<String, String> parsedPdfMap, Integer userId) {
        Optional<LkupPropertySource> propertySourceOpt = propertySourceRepository.findByName("dbGaP/MTA");
        if (propertySourceOpt.isEmpty()) {
            throw new CategoryNotFoundException("Could not find Property Source for dbGaP/MTA");
        }
        List<EntityProperty> pdfProperties = entityPropertyRepository.findAllByPropertySourceId(propertySourceOpt.get().getId());
        List<EntityPropertyMtaMapping> mtaMappings = mtaMappingRepository.findAll();
        List<LkupPropertyCodelistValue> codelistValuesList = codelistValueRepository.findAll();

        //map of property codelist id to a set of possible values
        Map<Integer, Set<String>> codelistValues = getCodelistValues(codelistValuesList);

        //map of property codelist id to a map of property codelist value id and value
        Map<Integer, Map<Integer, String>> codelistValuesMap = getCodelistValuesMap(codelistValuesList);

        //map of entity property ids to a list of associated mta mappings
        Map<Integer, List<EntityPropertyMtaMapping>> idToMtaMappings = mtaMappings.stream()
                .collect(Collectors.groupingBy(EntityPropertyMtaMapping::getEntityPropertyId));

        //true = one to one, false = codelisted (saves having to traverse and filter twice)
        Map<Boolean, List<EntityProperty>> splitEntityPropertyTypes = pdfProperties.stream()
                .collect(Collectors.partitioningBy(property -> property.getCodeListId() == null));

        //for any properties that aren't code listed
        List<StudyPropertyValue> oneToOneProperties = splitEntityPropertyTypes.get(true)
                .stream()
                .map(ep -> {
                    List<EntityPropertyMtaMapping> epMappings = idToMtaMappings.get(ep.getId());
                    //geno_seq_platform_info is a composite study entity property but not in the mapping so is null
                    if (epMappings == null) {
                        return new ArrayList<StudyPropertyValue>();
                    }
                    return epMappings.stream()
                            .map(mtaMap -> createNewPropertyValue(studyId, ep, mtaMap, parsedPdfMap))
                            .toList();
                })
                .flatMap(Collection::stream)
                .filter(spv -> spv.getPropertyValue() != null)
                .toList();

        List<EntityPropertyMtaMapping> commaSeparatedFields = new ArrayList<>(2);

        //processes anything that has a codelist
        //anything field marked 'comma-separated' will be added to a separate array and processed differently
        List<StudyPropertyValue> codelistedProperties = splitEntityPropertyTypes.get(false)
                .stream()
                .map(ep -> {
                    List<EntityPropertyMtaMapping> epMappings = idToMtaMappings.get(ep.getId());
                    return epMappings.stream().map(mtaMap -> {
                        String value = parsedPdfMap.get(mtaMap.getPdfFieldName());
                        return createNewCodelistedPropertyValue(value, studyId, ep, mtaMap,
                                codelistValuesMap, commaSeparatedFields
                        );
                    }).toList();
                })
                .flatMap(Collection::stream)
                .filter(spv -> spv.getPropertyValue() != null)
                .toList();

        //processes any field marked separated and creates an individual StudyPropertyValue for each parsed value
        List<StudyPropertyValue> commaSeparatedProperties = commaSeparatedFields.stream()
                .map(mtaMap ->
                        parseCommaSeparatedProperties(studyId, mtaMap, parsedPdfMap, codelistValues, pdfProperties)
                )
                .flatMap(Collection::stream)
                .filter(spv -> spv.getPropertyValue() != null)
                .toList();

        List<StudyPropertyValue> allPropertyValues = Stream.of(oneToOneProperties, codelistedProperties, commaSeparatedProperties)
                .flatMap(List::stream)
                .toList();

        allPropertyValues.forEach(spv -> spv.setCreatedBy(userId));

        return allPropertyValues;
    }

    /**
     * Maps certain study property values together based on their value index in the mta mapping table
     *
     * @param mtaMapDescription  column in the db where the value index is stored
     * @param studyPropertyValue
     */
    private void setValueIndex(String mtaMapDescription, StudyPropertyValue studyPropertyValue) {
        if (mtaMapDescription != null) {
            Matcher matcher = valueIndexMatcher.matcher(mtaMapDescription);
            if (matcher.find()) {
                studyPropertyValue.setValueIndex(Integer.valueOf(matcher.group(0)));
            } else {
                log.warn("No index numbers found.");
            }
        }
    }

    /**
     * Creates Study Property Value for a one-to-one entity property
     *
     * @param studyId
     * @param ep           entity property that the parsed value derives from
     * @param mtaMap       mta mapping table row containing data needed for the pdf->db relationship
     * @param parsedPdfMap map of data parsed from the MTA form
     * @return a new study property value entry
     */
    private StudyPropertyValue createNewPropertyValue(Integer studyId, EntityProperty ep, EntityPropertyMtaMapping mtaMap,
                                                      Map<String, String> parsedPdfMap) {
        String value = parsedPdfMap.get(mtaMap.getPdfFieldName());
        StudyPropertyValue studyPropertyValue = new StudyPropertyValue();
        if(value == null || value.isBlank()) {
            return studyPropertyValue;
        }
        studyPropertyValue.setStudyId(studyId);
        studyPropertyValue.setEntityProperty(ep);
        //geno_seq properties have the value index in the mta mapping description
        setValueIndex(mtaMap.getDescription(), studyPropertyValue);
        String description = mtaMap.getDescription();
        //study types has 2 mappings, types_other_specify has no codelist but isn't actually 1-to-1
        if (description != null && description.contains("comma-separated")) {
            value = null;
        } else if (mtaMap.getPdfFieldName().equals("Institutional Certifications") ||
                mtaMap.getPdfFieldName().equals("NHGRI Genomic Data Sharing  Submission Information")) {
            value = switch (value) {
                case "On" -> "Yes";
                case "Off" -> "No";
                default -> null;
            };
        }
        studyPropertyValue.setPropertyValue(value);
        return studyPropertyValue;
    }

    /**
     * Creates Study Property Value for a codelisted entity property
     *
     * @param value                value that should correspond to a codelist entry
     * @param studyId
     * @param ep                   entity property that the parsed value derives from
     * @param mtaMap               mta mapping table row containing data needed for the pdf->db relationship
     * @param codelistValuesMap    map of property codelist id to a map of property codelist value id and value
     * @param commaSeparatedFields mta mapping table entries that contain comma separated values
     * @return a new study property value entry
     */
    private StudyPropertyValue createNewCodelistedPropertyValue(String value, Integer studyId, EntityProperty ep,
                                                                EntityPropertyMtaMapping mtaMap,
                                                                Map<Integer, Map<Integer, String>> codelistValuesMap,
                                                                List<EntityPropertyMtaMapping> commaSeparatedFields) {
        StudyPropertyValue studyPropertyValue = new StudyPropertyValue();
        studyPropertyValue.setStudyId(studyId);
        studyPropertyValue.setEntityProperty(ep);
        value = processCodelistedValues(mtaMap, value, ep, codelistValuesMap, commaSeparatedFields);
        studyPropertyValue.setPropertyValue(value);
        return studyPropertyValue;
    }

    /**
     * Parses a comma separated form value and creates a study property value entity for each
     *
     * @param studyId
     * @param mtaMap         mta mapping table row containing data needed for the pdf->db relationship
     * @param parsedPdfMap   map of data parsed from the MTA form
     * @param codelistValues map of property codelist id to a map of property codelist value id and value
     * @param pdfProperties  a list of all entity properties associated with the MTA form PDF
     * @return a list of newly created study value property entities
     */
    private List<StudyPropertyValue> parseCommaSeparatedProperties(Integer studyId, EntityPropertyMtaMapping mtaMap,
                                                                   Map<String, String> parsedPdfMap,
                                                                   Map<Integer, Set<String>> codelistValues,
                                                                   List<EntityProperty> pdfProperties) {
        String values = parsedPdfMap.get(mtaMap.getPdfFieldName());
        if(values.trim().isBlank()) {
            return new ArrayList<>(0);
        }
        //TODO: this will parse incorrectly if a value has a comma
        // - might want to switch this from comma separated to colon separated
        List<String> splitValues = Arrays.stream(values.split(",")).toList();
        List<StudyPropertyValue> studyPropertyValueList = new ArrayList<>();
        for (String value : splitValues) {
            String updatedValue = value.trim();
            StudyPropertyValue studyPropertyValue = new StudyPropertyValue();
            studyPropertyValue.setStudyId(studyId);
            Set<String> codelistValuesSet = codelistValues.get(mtaMap.getCodelistId());
            EntityProperty ep;
            Optional<String> clValue = codelistValuesSet.stream().filter(updatedValue::equalsIgnoreCase).findAny();

            //weird logic since there are 2 entity properties for study types
            //one for codelisted types, one for anything else
            //institutes_supporting_study should always find a codelisted value
            if (clValue.isPresent()) {
                updatedValue = clValue.get();
                ep = pdfProperties.stream()
                        .filter(prop -> prop.getCodeListId() != null && prop.getCodeListId().equals(mtaMap.getCodelistId()))
                        .findAny().orElse(null);
            } else {
                if (mtaMap.getPdfFieldName().contains("types")) {
                    ep = pdfProperties.stream()
                            .filter(prop -> prop.getCodeListId() == null && prop.getName().equals("types_other_specify"))
                            .findAny().orElse(null);
                } else {
                    //supporting institutes need to match
                    log.error("Institute not on codelist: {}", updatedValue);
                    throw new PdfParsingException("Could not find valid mapping for: " + mtaMap.getPdfFieldName());
                }

            }
            studyPropertyValue.setEntityProperty(ep);
            studyPropertyValue.setPropertyValue(updatedValue);
            studyPropertyValueList.add(studyPropertyValue);
        }

        return studyPropertyValueList;
    }

    /**
     * @param mtaMap               mta mapping table row containing data needed for the pdf->db relationship
     * @param value                value that should correspond to a codelist entry
     * @param ep                   entity property that the parsed value derives from
     * @param codelistValuesMap    map of property codelist id to a map of property codelist value id and value
     * @param commaSeparatedFields mta mapping table entries that contain comma separated values
     * @return value to be persisted in the database
     */
    private String processCodelistedValues(EntityPropertyMtaMapping mtaMap, String value, EntityProperty ep,
                                           Map<Integer, Map<Integer, String>> codelistValuesMap,
                                           List<EntityPropertyMtaMapping> commaSeparatedFields) {
        if (mtaMap.getCodelistValueId() != null) {
            String codelistValue = codelistValuesMap.get(ep.getCodeListId()).get(mtaMap.getCodelistValueId());
            if (value == null) {
                return value;
            }
            value = value.trim();
            value = switch (value) {
                case "On" -> codelistValue;
                case "null", "Off" -> null;
                //this radio button maps to strings instead of yes/no or on/off
                case "generation", "publication" -> {
                    if (mtaMap.getPdfFieldName().equals("Data submission radio")) {
                        if (mtaMap.getDescription().equals(value)) {
                            yield codelistValue;
                        }
                        yield null;
                    }
                    //in case another properties value happens to match return the value
                    //might have to change this to the codelist value or some other logic if this actually happens
                    log.warn("Please check {} value for this study. Might contain non-codelisted value.", ep.getName());
                    yield value;
                }
                default -> value;
            };

        } else {
            String description = mtaMap.getDescription();
            if (description != null && description.contains("comma-separated")) {
                commaSeparatedFields.add(mtaMap);
                value = null;
            }
            else if(value.equals("Off")) {
                value = null;
            }
        }
        return value;
    }

    private Map<Integer, Set<String>> getCodelistValues(List<LkupPropertyCodelistValue> codelistValuesList) {
        return codelistValuesList.stream()
                .collect(Collectors.groupingBy(
                        LkupPropertyCodelistValue::getPropertyCodelistId,
                        Collectors.mapping(LkupPropertyCodelistValue::getValue, Collectors.toSet()))
                );
    }

    private Map<Integer, Map<Integer, String>> getCodelistValuesMap(List<LkupPropertyCodelistValue> codelistValuesList) {
        return codelistValuesList.stream()
                .collect(Collectors.groupingBy(
                        LkupPropertyCodelistValue::getPropertyCodelistId,
                        Collectors.toMap(
                                LkupPropertyCodelistValue::getId,
                                LkupPropertyCodelistValue::getValue
                        ))
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

        List<LkupPropertySource> propertySources = switch (role) {
            case "Curator" -> propertySourceRepository.findAllByNameIn(CURATOR_PROPERTY_SOURCES);
            case "DCC" -> propertySourceRepository.findAllByNameIn(DCC_PROPERTY_SOURCES);
            default -> throw new BadDataException("Invalid editing role");
        };

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
        //true for property values to delete, false for property values to edit
        Map<Boolean, List<StudyPropertyValue>> shouldBeRemoved = propertyValues.stream()
                .collect(Collectors.partitioningBy(StudyPropertyValue::getShouldBeRemoved));

        checkForNullEntityProperties(shouldBeRemoved.get(false));
        deleteStudyValueProperties(shouldBeRemoved.get(true));
        List<LkupPropertyCodelistValue> codelistValuesList = codelistValueRepository.findAll();

        for (StudyPropertyValue spv : shouldBeRemoved.get(false)) {
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

        if (shouldSubmit) {
            updateStatus(studyOpt.get(), role, userId);
        }
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
     */
    private void updateStatus(Study study, String role, Integer userId) {
        switch (role) {
            case "Curator" -> {
                LkupStatus status = statusRepository.findByUsageAndName(Constants.USAGE_STUDY, Constants.STATUS_APPROVED_STUDY)
                        .orElseThrow(()  -> new StatusNotFoundException(String.format("Could not find study status entity. Invalid Study Status: %s", Constants.STATUS_APPROVED_STUDY)));
                study.setStatus(status);
                //update release date property value
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
                StudyPropertyValue releaseDate = createPropertyValue(study.getId(), userId, "release_date", formatter.format(LocalDate.now()));
                studyPropertyValueRepository.save(releaseDate);
                studyRepository.save(study);
//                emailRequestService.sendStudyRegEmail(study.getId(), StudyRegEmailType.NEW_STUDY_APPROVAL);
            }
            case "DCC" -> {
                LkupStatus status = statusRepository.findByUsageAndName(Constants.USAGE_STUDY, Constants.STATUS_IN_REVIEW)
                        .orElseThrow(()  -> new StatusNotFoundException(String.format("Could not find study status entity. Invalid Study Status: %s", Constants.STATUS_IN_REVIEW)));
                study.setStatus(status);
                studyRepository.save(study);
//                emailRequestService.sendStudyRegEmail(study.getId(), StudyRegEmailType.NEW_STUDY_DCC_METADATA);
            }
            default -> throw new BadDataException("Invalid role when updating study status");
        }
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
    public List<UserStudyRegistrationDTO> getUserStudiesByDcc(Integer userId, String status) {
        Users users = usersRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("No user found for user ID " + userId));
        if(users.getDcc() == null || users.getDcc().getId() == null){
            throw new UserAuthorizationException("User is not aligned to a DCC");
        }

        // Find dcc studies by status, ordering by status, creation date
        //check if status is valid
        statusRepository.findByName(status)
                .orElseThrow(()  -> new StatusNotFoundException(String.format("Invalid Study Status: %s", status)));
        List<ViewStudy> viewStudies = viewStudyRepository.findDCCStudiesByStatus(users.getDcc().getId(), status);
        return viewStudyDccMapper.toDTOs(viewStudies);
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
        return viewStudyDccMapper.toDTOs(studies);
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
        Optional<EntityProperty> ep = entityPropertyRepository.findByName(name);
        if (ep.isEmpty()) {
            throw new CategoryNotFoundException("Could not find entity property");
        }
        studyPropertyValue.setEntityProperty(ep.get());
        return studyPropertyValue;
    }



}
