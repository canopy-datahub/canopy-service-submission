package ex.org.project.submissionService.services;

import com.amazonaws.services.macie2.AmazonMacie2;
import com.amazonaws.services.macie2.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.bmir.radx.datadictionary.lib.*;
import edu.stanford.bmir.radx.metadata.validator.lib.LiteralFieldValidators;
import edu.stanford.bmir.radx.metadata.validator.lib.ValidatorFactory;
import ex.org.project.submissionService.exceptions.custom.*;
import ex.org.project.submissionService.models.*;
import ex.org.project.submissionService.models.ValidationResult;
import ex.org.project.submissionService.models.dtos.ValidationResultsDTO;
import ex.org.project.submissionService.repositories.*;
import ex.org.project.submissionService.utils.BundlingUtils;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.SfnException;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse;
import software.amazon.awssdk.services.sqs.model.Message;


import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Service
@ComponentScan(basePackages = {"edu.stanford.bmir.radx.datadictionary.lib", "edu.stanford.bmir.radx.metadata.validator"})
public class ValidationService {

    private final StudyRepository studyRepository;
    private final DataSubmissionRepository dataSubmissionRepository;
    private final DataFileRepository dataFileRepository;
    private final LkupDCCRepository lkupDCCRepository;
    private final AwsStorageService awsStorageService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private Validator dataDictionaryValidator;

    @Autowired
    private ValidatorFactory metadataValidatorFactory;

    @Value("${radx.accountNumber}")
    private String accountNumber;

    @Value("${radx.stepFnMachineArn}")
    private String machineArn;

    /**
     * getValidationResults returns a list of all validation results for the files that failed validation in a submission
     * if any files have not completed validation yet, no results will be returned
     * @param submissionId is the submission id
     * @return list of ValidationResult
     */
    public ValidationResultsDTO getSubmissionValidationResults(Integer submissionId) {
        //check if submission id is valid
        dataSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionIdInvalidException("Could not find submission ID: " + submissionId));

        ValidationResultsDTO validationResultsDTO = new ValidationResultsDTO();
        validationResultsDTO.setSubmissionId(submissionId);

        List<ValidationResult> bundles = getBundlesValidationResults(submissionId);
        validationResultsDTO.setBundles(bundles);
        return validationResultsDTO;
    }

    private List<ValidationResult> getBundlesValidationResults(Integer submissionId) {
        List<DataFile> validatedFiles = dataFileRepository.findBySubmissionId(submissionId);
        List<DataFile> parentDataFiles = BundlingUtils.popParentFilesFromList(validatedFiles);
        BundlingUtils.popDocumentsFromList(validatedFiles);
        Map<Integer, List<Integer>> bundleIdMapping = BundlingUtils.getBundleIdMapping(parentDataFiles);
        List<ValidationResult> parentResults = getValidationResults(parentDataFiles);
        for(ValidationResult bundle : parentResults) {
            List<DataFile> childFiles = BundlingUtils.popChildrenFromList(bundle.getFileId(), bundleIdMapping, validatedFiles);
            updateBundleChildren(bundle, childFiles);
        }
        return parentResults;
    }

    private void updateBundleChildren(ValidationResult bundle, List<DataFile> childFiles) {
        List<ValidationResult> bundleChildFiles = getValidationResults(childFiles);
        bundle.setChildFiles(bundleChildFiles);
    }

    /**
     * Returns a list of validation results for a file
     *
     * @param dataFileId ID of the DataFile entity you are checking for validation results
     * @return list of validation results for the specified DataFile
     */
    public ValidationResultsDTO getFileValidationResults(Integer dataFileId) {
        DataFile dataFile = dataFileRepository.findById(dataFileId)
                .orElseThrow(() -> new DataFileNotFoundException("No DataFile found with ID: " + dataFileId));
        ValidationResultsDTO validationResultsDTO = new ValidationResultsDTO();
        validationResultsDTO.setSubmissionId(dataFile.getSubmissionId());
        var validationResults = getValidationResults(List.of(dataFile));
        validationResultsDTO.setBundles(validationResults);
        return validationResultsDTO;
    }

    private List<ValidationResult> getValidationResults(List<DataFile> dataFiles) {
        return dataFiles.stream()
                .map(this::getFileValidationResult)
                .toList();
    }

    /**
     * getFileValidationResult returns the validation results of the file and updates the 'acknowledged' flag
     *
     * @param dataFile is the datafile
     * @return ValidationResult representation of string
     */
    @Transactional
    public ValidationResult getFileValidationResult(DataFile dataFile) {
        try {
            ValidationResult results;
            //if validation errors exist, get from db
            if(dataFile.getValidationResults()!= null){
                results =   mapper.readValue(dataFile.getValidationResults(), ValidationResult.class);
            }
            //create validation result for file with no errors
            else{
                results = new ValidationResult(dataFile);
            }
            results.setAcknowledged(dataFile.getValidationAcknowledged());
            return results;

        } catch (JsonProcessingException e) {
            throw new ValidationErrorException("Failed to convert json string to validation result object: " + e.getMessage());
        }
    }

    /**
     * validateFiles performs file validations on file based on the file type
     * @param submissionId is the submissionId
     * @return true if validation completed
     */
    public boolean validateFiles(Integer submissionId) {
        //check if submission id is valid
        dataSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionIdInvalidException("Could not find submission ID: " + submissionId));
        List<DataFile> filesToValidate = dataFileRepository.findBySubmissionId(submissionId);
        //validation only for data, dict and meta files
        BundlingUtils.popDocumentsFromList(filesToValidate);
        if(filesToValidate.isEmpty()) {
            return true;
        }

        log.info("number of files found for : " + filesToValidate.size());
        for (DataFile file : filesToValidate) {
          validateFile(file, file.getFileCategory().getName());
            }

        return true;
    }

    /**
     * Checks whether a DataFile is a transform file, and runs validation if it is
     *
     * @param dataFile The datafile to be validated
     * @return boolean - true if validation was completed, false otherwise
     */
    public boolean validateIfTransformFile(DataFile dataFile) {
        if (!dataFile.getFileCategory().getName().equals(Constants.HARMONIZED_DATAFILE)) {
            return false;
        }
        try {
            performCDEValidation(dataFile);
            return true;
        } catch (ValidationErrorException e) {
            return false;
        }
    }

    /**
     * validateFile performs file validations on file based on the file type
     * @param dataFile The datafile to be validated
     */
    public void validateFile(DataFile dataFile, String dataFileCategory) {
        try {
            switch (dataFileCategory) {
                case Constants.HARMONIZED_DATAFILE -> {
                    performCDEValidation(dataFile);
                }
                case Constants.NON_HARMONIZED_DICTFILE , Constants.HARMONIZED_DICTFILE -> performDataDictionaryValidation(dataFile);
                case Constants.NON_HARMONIZED_METAFILE, Constants.HARMONIZED_METAFILE -> performMetadataValidation(dataFile);
            }

        } catch (ValidationErrorException e) {
            log.info("Error occurred during validation: " + e.getMessage());
            throw new ValidationErrorException("Error occurred during validation: " + e.getMessage());
        }
    }

    /**
     * performCDEValidation finds the associated S3 file, perform cde validation on its contents
     * and sets the file headers and validation results on it's datafile object
     *
     * @param dataFile    is the on which to be validated
     */
    private void performCDEValidation(DataFile dataFile) {
        log.info("ABOUT TO PERFORM CDE VALIDATION FOR: " + dataFile.getSourceFileName());
        S3File s3File = dataFile.getS3File();
        if (s3File == null) {
            throw new DataFileNotFoundException("No S3 file found for ID " + dataFile.getId());
        }

        InputStream object = Objects.requireNonNull(awsStorageService.getS3FileContent(s3File));

        //get s3 file key from path to access file in s3 needed for validation
        CDEValidator cdeValidator = new CDEValidator(dataFile, object);
        String  results = null;
        try {
            //get the validation result object from cde validator
            ValidationResult dataFileValidationResults  = cdeValidator.getCDEValidationResult();
            //get the string representation of json result. It is already set in CDEValidator so validation type should be "n/a"
            results = updateValidationResultErrors(dataFileValidationResults,"n/a",dataFileValidationResults.getCdeErrors());
        } catch (Exception e) {
            log.warn("Error occurred during CDE validation: ", e.getMessage());
            throw new ValidationErrorException("Error occurred during CDE validation: " + e.getMessage());
        }
        dataFile.setValidationResults(results);
        dataFileRepository.saveAndFlush(dataFile);

    }

    /**
     * Gets Radx Metadata Specification metadata schema template from the resources folder
     */
    private InputStream getTemplateSchema() {
        String fileName = "Template.json";
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
        // the stream holding the file content
        if (inputStream != null) {
            return inputStream;
        } else {
            throw new BadDataException("RADx Metadata Specification metadata schema template " + fileName + " not found.");
        }
    }

    /**
     * performMetadataValidation finds the associated S3 file, perform metadata validation against the template schema
     * referenced in the file
     * @param metadataFile is the on which to be validated
     */
    public void performMetadataValidation(DataFile metadataFile){
        log.info("ABOUT TO PERFORM METADATA VALIDATION FOR: " + metadataFile.getSourceFileName());
        S3File s3File = metadataFile.getS3File();
        if (s3File == null) {
            throw new DataFileNotFoundException("No S3 file found for ID " + metadataFile.getId());
        }

        try {
            InputStream inputStream = awsStorageService.getS3FileContent(s3File);
            //create a metadata validator instance
            edu.stanford.bmir.radx.metadata.validator.lib.Validator metadataValidator = metadataValidatorFactory.createValidator(new LiteralFieldValidators(new HashMap<>()));
            //convert the contents of the metadata file to string
            String metadataFileString = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            //loads the RADx Metadata Specification as the template schema
            String templateSchema = IOUtils.toString(getTemplateSchema(), StandardCharsets.UTF_8);

            //validate the file against the schema template
            var validationReport = metadataValidator.validateInstance(templateSchema, metadataFileString);
            var validationResults = validationReport.results();

            // Count the number of validation results that are ERRORS
            int errorCount = (int) validationResults.stream()
                    .filter(validationResult -> validationResult.validationLevel().equals(edu.stanford.bmir.radx.metadata.validator.lib.ValidationLevel.ERROR))
                    .count();
            log.info("There are " + errorCount + " errors");

            // sets validation flag based on error count
            metadataFile.setMetaValidationFailed(errorCount > 0);

            //convert the validation results into a ValidationResultToDTO and save results to db
            processMetadataValidationResultToDTO(metadataFile, validationReport, errorCount);
            dataFileRepository.save(metadataFile);

        } catch (Exception e) {
            throw new ValidationErrorException("Error occurred during metadata validation: "+  e.getMessage());
        }
    }

    /**
     * performDataDictionaryValidation finds the associated S3 file, perform dictionary validation on its contents
     * @param dictionaryFile is the on which to be validated
     */
	public void performDataDictionaryValidation(DataFile dictionaryFile) {

		log.info("ABOUT TO PERFORM Dictionary VALIDATION FOR: " + dictionaryFile.getSourceFileName());
		S3File s3File = dictionaryFile.getS3File();
		if (s3File == null) {
			throw new DataFileNotFoundException("No S3 file found for ID " + dictionaryFile.getId());
		}

		try {
			InputStream in = awsStorageService.getS3FileContent(s3File);
            // Parse our data dictionary csv
			CsvParser parser = new CsvParser();
			Csv csv = parser.parseCsv(in);
			// Validate the data dictionary
			var validationReport = dataDictionaryValidator.validateDataDictionary(csv);
            //get errors of all levels - Validation level:  ERROR, WARNING, INFO.
            // WARNING and INFO provide guidance on how to clean up a data dictionary
            List<edu.stanford.bmir.radx.datadictionary.lib.ValidationResult> validationResults = validationReport.results();

			// Process the validation report as needed
			int errorCount = validationResults.size();
			log.info("There are " + errorCount + " errors");

			// sets validation flag based on error count
			dictionaryFile.setDictValidationFailed(errorCount > 0);

			processValidationResultToDTO(dictionaryFile, validationResults, errorCount);

			dataFileRepository.save(dictionaryFile);
		} catch (IOException e) {
			throw new ValidationErrorException("Error occurred during dictionary validation: "+  e.getMessage());
		}
	}

    /**
     * processValidationResultToDTO converts the results from ValidationReport to a Validation Result Object
     * @param dictionaryFile is the validated file
     * @param validationReportResults is the dictionary validator's report results
     */
    private void processValidationResultToDTO(DataFile dictionaryFile, List<edu.stanford.bmir.radx.datadictionary.lib.ValidationResult> validationReportResults, int errorCount){
        //Create instance of Validation Result for file
        ValidationResult validationResults = new ValidationResult(dictionaryFile);
        validationResults.setDataEntryWarningCount(errorCount);
        MultiValuedMap<String, ValidationError> errors = new ArrayListValuedHashMap<>();
        //loop through each result and convert it into validation error
        // Each report contains a list of results
        validationReportResults
                .forEach(validationResult -> {
                    // For each validation result we have:
                    // Validation level:  ERROR, WARNING, INFO.  WARNING and INFO provide
                    var validationLevel = validationResult.validationLevel().name();

                    // The zero-based index of the row in the data dictionary CSV that
                    // the result pertains to
                    // Note that 0 is the header row
                    var rowNumber = validationResult.csvRow().rowIndex() + 1;

                    // The name of the validation result
                    var name = validationResult.name();

                    // The message of the validation result
                    var message = validationResult.message();

                    errors.put(validationResult.name(), new ValidationError("Data Dictionary Validation",validationResult.subject(), validationLevel, null,(long) rowNumber, name, message));

                });

        String validationString = updateValidationResultErrors(validationResults,"dict",errors.asMap());
        dictionaryFile.setValidationResults(validationString);

    }

    /**
     * processMetadataValidationResultToDTO converts the results from the metadata validator's ValidationReport to a Validation Result Object
     * @param dictionaryFile is the validated file
     * @param validationReport is the dictionary validator's report
     */
    private void processMetadataValidationResultToDTO(DataFile dictionaryFile, edu.stanford.bmir.radx.metadata.validator.lib.ValidationReport validationReport, int errorCount){
        //Create instance of Validation Result for file
        ValidationResult validationResults = new ValidationResult(dictionaryFile);
        validationResults.setValidationType("Metadata File Validation");
        validationResults.setDataEntryWarningCount(errorCount);
        MultiValuedMap<String, ValidationError> errors = new ArrayListValuedHashMap<>();
        //loop through each result and convert it into validation error
        // Each report contains a list of results
        validationReport.results()
                .forEach(validationResult -> {
                    // For each validation result we have:
                    // Validation level:  ERROR, WARNING, INFO.  WARNING and INFO provide
                    var validationLevel = validationResult.validationLevel().name();

                    // The name of the validation result
                    var name = validationResult.validationName().name();

                    // The message of the validation result
                    var message = validationResult.message();

                    String solution = "Please ensure that the metadata instances adhere to the standards defined in the RADx Metadata Template, which can be found in the RADx Metadata Specification Docs.";

                    errors.put(name, new ValidationError(name,validationResult.pointer(), validationLevel, null,null, message, solution));

                });

        String validationString = updateValidationResultErrors(validationResults,"meta",errors.asMap());
        dictionaryFile.setValidationResults(validationString);
    }

    /**
     * Updates the validation acknowledgement status of the files in a submission.
     *
     * @param dto The DTO containing the validation results and submission ID
     * @return {true} if the validation acknowledgement was successfully updated,
     * {false} if no files were found for the submission
     */
    @Transactional
    public Boolean updateFileAck(ValidationResultsDTO dto, Integer userId) {
        // Extract the validation result file IDs
        List<ValidationResult> childResults = dto.getBundles().stream()
                .map(ValidationResult::getChildFiles)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList();
        List<ValidationResult> validationResults = Stream.of(dto.getBundles(), childResults)
                .flatMap(Collection::stream)
                .toList();

        // Extract DataFile ids for files we want to "acknowledge" warnings
        List<Integer> ackFileIdsInRequest = validationResults.stream()
                .filter(ValidationResult::getAcknowledged)
                .map(ValidationResult::getFileId)
                .toList();

        // Find all data files related to the submission
        List<DataFile> submissionFiles = dataFileRepository.findBySubmissionId(dto.getSubmissionId());

        // If no files found, return false
        if (submissionFiles.isEmpty()) {
            return false;
        }

        // Filter the submission files based on the requested file IDs and validation results
        List<DataFile> filesToAck = submissionFiles.stream()
                .filter(df -> ackFileIdsInRequest.contains(df.getId()))
                .filter(havingAnyValidationFailed())
                .toList();

        filesToAck.forEach(df -> {
            df.setValidationAcknowledged(true);
            df.setModifiedBy(userId);
        });

        // Check for files which we didn't receive in the request but are in the database
        if (submissionFiles.stream()
                .filter(df -> !ackFileIdsInRequest.contains(df.getId()))
                .filter(havingAnyValidationFailed())
                .anyMatch(df -> df.getValidationAcknowledged() == null || !df.getValidationAcknowledged())) {
            return false;
        }

        return true;
    }

    /**
     * Return a datafile that failed any validation
     */
	private static Predicate<DataFile> havingAnyValidationFailed() {
		return df -> (df.getCdeValidationFailed() != null && df.getCdeValidationFailed())
				|| (df.getMetaValidationFailed() != null && df.getMetaValidationFailed())
				|| (df.getDictValidationFailed() != null && df.getDictValidationFailed());
	}

    /**
     * Updates the isValidated status of a submission.
     * @param submissionId is the submission ID
     * @isValidated {true} if the validation process completed, {false} if error occurred during validation
     */
    @Transactional
    public void updateSubmissionIsValidated(Integer submissionId, Boolean isValidated) {
        DataSubmission submission = dataSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionIdInvalidException("Could not find submission ID: " + submissionId));
        submission.setValidated(isValidated);
        dataSubmissionRepository.save(submission);
    }

    /**
     * updateValidationResultErrors updated the Validation result errors based on the validation type
     * @param validationResult is the current Validation Result object associated with the file
     * @param validationType is the type of validation result errors to be updated
     * @param errors are the validation specific errors collected
     * @return the updated string representation of Validation Result
     */
    private String updateValidationResultErrors(ValidationResult validationResult, String validationType, Map<String, Collection<ValidationError>> errors){
        switch (validationType){
            case "cde" -> validationResult.setCdeErrors(errors);
            case "dict" -> validationResult.setDictErrors(errors);
            case "meta" -> validationResult.setMetaErrors(errors);
            default -> {
                break;
            }
        }

        // Creating the ObjectMapper object
        ObjectMapper mapper = new ObjectMapper();
        String jsonString;
        // Converting the Object to JSONString
        try {
            jsonString = mapper.writeValueAsString(validationResult);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return jsonString;
    }

}



