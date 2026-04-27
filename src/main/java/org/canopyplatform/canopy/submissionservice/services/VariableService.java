package org.canopyplatform.canopy.submissionservice.services;

import org.canopyplatform.canopy.submissionservice.exceptions.custom.BadDataException;
import org.canopyplatform.canopy.submissionservice.exceptions.custom.SubmissionIdInvalidException;
import org.canopyplatform.canopy.submissionservice.models.*;
import org.canopyplatform.canopy.submissionservice.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VariableService {

    private final VariableRepository variableRepository;
    private final LkupCoreVariablePropertyValueRepository coreVariablePropertyValueRepository;
    private final EntityPropertyRepository entityPropertyRepository;
    private final DataSubmissionRepository dataSubmissionRepository;
    private final StudyRepository studyRepository;
    private final LkupVariableCategoryRepository lkupVariableCategoryRepository;

    // Entity property names for core variable metadata
    private static final String PROPERTY_VARIABLE_LABEL = "variable_label";
    private static final String PROPERTY_SECTION = "section";
    private static final String PROPERTY_DATATYPE = "datatype";
    private static final String PROPERTY_VARIABLE_DESCRIPTION = "variable_description";
    private static final String PROPERTY_VARIABLE_UNIT = "variable_unit";
    private static final String PROPERTY_VARIABLE_CARDINALITY = "variable_cardinality";
    private static final String PROPERTY_VARIABLE_TERM = "variable_term";
    private static final String PROPERTY_VARIABLE_KEYWORDS = "variable_keywords";

    /**
     * Updates the variables table for a given DataFile
     * @param dataFile The DataFile to process
     * @throws BadDataException if dataFile is null or has invalid data
     * @throws SubmissionIdInvalidException if submission is not found
     */
    @Transactional
    public void updateVariablesForDataFile(DataFile dataFile) {
        if (dataFile == null) {
            throw new BadDataException("DataFile is null, cannot update variables");
        }

        // Delete existing variables for this file
        variableRepository.deleteByFileId(dataFile.getId());

        // Get variable names from file headers
        List<String> variableNames = parseVariableNamesFromDataFile(dataFile);
        if (variableNames.isEmpty()) {
            log.debug("No variables found in DataFile {}", dataFile.getId());
            return;
        }

        // Get study and center information
        Integer studyId = getStudyIdFromDataFile(dataFile);
        Integer centerId = getCenterIdFromStudy(studyId);

        // Fetch all core variable names once
        HashSet<String> coreVariableNames = getAllCoreVariableNames();

        // Fetch entity property IDs once
        Map<String, Integer> propertyIdMap = getEntityPropertyIdMap();

        // Process each variable
        for (String variableName : variableNames) {
            if (variableName == null || variableName.trim().isEmpty()) {
                continue;
            }

            Variable variable = new Variable();
            variable.setName(variableName);
            variable.setFile(dataFile);

            // Set study and center if available
            if (studyId != null) {
                Study study = studyRepository.findById(studyId).orElse(null);
                variable.setStudy(study);
            }
            if (centerId != null) {
                LkupCenter center = new LkupCenter();
                center.setId(centerId);
                variable.setCenter(center);
            }

            // Check if this is a core variable
            boolean isCoreVariable = isCoreVariable(variableName, coreVariableNames);
            if (isCoreVariable) {
                // Set category_id = 1 for core variables
                LkupVariableCategory coreCategory = lkupVariableCategoryRepository.findById(1).orElse(null);
                variable.setCategory(coreCategory);

                // Populate metadata from lkup_core_variable_property_value
                populateCoreVariableMetadata(variable, variableName, propertyIdMap);
            } else{
                LkupVariableCategory nonCoreCategory = lkupVariableCategoryRepository.findById(2).orElse(null);
                variable.setCategory(nonCoreCategory);
            }

            // Save the variable
            try {
                variableRepository.save(variable);
                log.debug("Saved variable: {} for file: {}", variableName, dataFile.getId());
            } catch (Exception e) {
                throw new BadDataException("Error saving variable " + variableName + " for file " + dataFile.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Parses variable names from the DataFile's fileHeaders field
     * @param dataFile The DataFile to parse
     * @return List of variable names
     */
    private List<String> parseVariableNamesFromDataFile(DataFile dataFile) {
        String fileHeaders = dataFile.getFileHeaders();
        if (fileHeaders == null || fileHeaders.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // File headers are stored as semicolon-separated values
        String[] headers = fileHeaders.split(";");
        List<String> variableNames = new ArrayList<>();
        for (String header : headers) {
            String trimmed = header.trim();
            if (!trimmed.isEmpty()) {
                variableNames.add(trimmed);
            }
        }
        return variableNames;
    }

    /**
     * Gets the study ID from a DataFile via its submission
     * @param dataFile The DataFile
     * @return The study ID, or null if not found
     * @throws SubmissionIdInvalidException if submission ID is found but submission doesn't exist
     */
    private Integer getStudyIdFromDataFile(DataFile dataFile) {
        Integer submissionId = dataFile.getSubmissionId();
        if (submissionId == null) {
            log.debug("No submission ID found for DataFile {}", dataFile.getId());
            return null;
        }

        DataSubmission submission = dataSubmissionRepository.findDataSubmissionById(submissionId);
        if (submission == null) {
            throw new SubmissionIdInvalidException("No submission found for ID " + submissionId);
        }

        return submission.getStudyId();
    }

    /**
     * Gets the center ID from a study
     * @param studyId The study ID
     * @return The center ID, or null if not found
     */
    private Integer getCenterIdFromStudy(Integer studyId) {
        if (studyId == null) {
            return null;
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null || study.getCenter() == null) {
            log.debug("No center found for study {}", studyId);
            return null;
        }

        return study.getCenter().getId();
    }

    /**
     * Fetches all core variable names from the lkup_core_variable_property_value table
     * Similar to how hasTier1Variable gets all tier1 variables at once
     * @return HashSet of all core variable names
     */
    private HashSet<String> getAllCoreVariableNames() {
        List<LkupCoreVariablePropertyValue> allCoreVariables =
            coreVariablePropertyValueRepository.findAll();

        HashSet<String> coreVariableNames = new HashSet<>();
        for (LkupCoreVariablePropertyValue coreVar : allCoreVariables) {
            coreVariableNames.add(coreVar.getVariableName());
        }

        return coreVariableNames;
    }

    /**
     * Checks if a variable is a core variable by checking if it exists in the core variables set
     * Similar to how hasTier1Variable checks intersection with tier1 variables
     * @param variableName The variable name to check
     * @param coreVariableNames HashSet of all core variable names
     * @return true if the variable is a core variable
     */
    private boolean isCoreVariable(String variableName, HashSet<String> coreVariableNames) {
        return coreVariableNames.contains(variableName);
    }

    /**
     * Fetches entity property IDs for all required properties
     * @return Map of property name to property ID
     */
    private Map<String, Integer> getEntityPropertyIdMap() {
        List<String> propertyNames = Arrays.asList(
            PROPERTY_VARIABLE_LABEL,
            PROPERTY_SECTION,
            PROPERTY_DATATYPE,
            PROPERTY_VARIABLE_DESCRIPTION,
            PROPERTY_VARIABLE_UNIT,
            PROPERTY_VARIABLE_CARDINALITY,
            PROPERTY_VARIABLE_TERM,
            PROPERTY_VARIABLE_KEYWORDS
        );

        Map<String, Integer> propertyIdMap = new HashMap<>();
        for (String propertyName : propertyNames) {
            entityPropertyRepository.findByName(propertyName).ifPresent(
                entityProperty -> propertyIdMap.put(propertyName, entityProperty.getId())
            );
        }

        return propertyIdMap;
    }

    /**
     * Populates core variable metadata from lkup_core_variable_property_value table
     * @param variable The variable to populate
     * @param variableName The variable name
     * @param propertyIdMap Map of property names to IDs
     */
    private void populateCoreVariableMetadata(Variable variable, String variableName,
                                              Map<String, Integer> propertyIdMap) {
        // Fetch all property values for this variable
        List<LkupCoreVariablePropertyValue> propertyValues =
            coreVariablePropertyValueRepository.findByVariableName(variableName);

        // Create a map of entity_property_id to property_value for quick lookup
        Map<Integer, String> valueMap = new HashMap<>();
        for (LkupCoreVariablePropertyValue pv : propertyValues) {
            valueMap.put(pv.getEntityPropertyId(), pv.getPropertyValue());
        }

        // Populate each metadata field
        if (propertyIdMap.containsKey(PROPERTY_VARIABLE_LABEL)) {
            Integer labelId = propertyIdMap.get(PROPERTY_VARIABLE_LABEL);
            variable.setLabel(valueMap.get(labelId));
        }

        if (propertyIdMap.containsKey(PROPERTY_SECTION)) {
            Integer sectionId = propertyIdMap.get(PROPERTY_SECTION);
            variable.setSection(valueMap.get(sectionId));
        }

        if (propertyIdMap.containsKey(PROPERTY_DATATYPE)) {
            Integer datatypeId = propertyIdMap.get(PROPERTY_DATATYPE);
            variable.setDatatype(valueMap.get(datatypeId));
        }

        if (propertyIdMap.containsKey(PROPERTY_VARIABLE_DESCRIPTION)) {
            Integer descriptionId = propertyIdMap.get(PROPERTY_VARIABLE_DESCRIPTION);
            variable.setDescription(valueMap.get(descriptionId));
        }

        if (propertyIdMap.containsKey(PROPERTY_VARIABLE_UNIT)) {
            Integer unitId = propertyIdMap.get(PROPERTY_VARIABLE_UNIT);
            variable.setUnit(valueMap.get(unitId));
        }

        if (propertyIdMap.containsKey(PROPERTY_VARIABLE_CARDINALITY)) {
            Integer cardinalityId = propertyIdMap.get(PROPERTY_VARIABLE_CARDINALITY);
            variable.setCardinality(valueMap.get(cardinalityId));
        }

        if (propertyIdMap.containsKey(PROPERTY_VARIABLE_TERM)) {
            Integer termId = propertyIdMap.get(PROPERTY_VARIABLE_TERM);
            variable.setTerms(valueMap.get(termId));
        }

        if (propertyIdMap.containsKey(PROPERTY_VARIABLE_KEYWORDS)) {
            Integer keywordsId = propertyIdMap.get(PROPERTY_VARIABLE_KEYWORDS);
            variable.setKeywords(valueMap.get(keywordsId));
        }
    }
}

