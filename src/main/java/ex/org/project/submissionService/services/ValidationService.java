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
    private final S3FileRepository s3FileRepository;
    private final LkupDCCRepository lkupDCCRepository;
    private final AwsStorageService awsStorageService;
    private final AmazonMacie2 macieClient;
    private final SfnClient sfnClient;
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
     * if any files have not completed validation yet (i.e. pii_phi_failed is null), no results will be returned
     * @param submissionId is the submission id
     * @return list of ValidationResult
     */
    public ValidationResultsDTO getSubmissionValidationResults(Integer submissionId) {
        //check if submission id is valid
        dataSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionIdInvalidException("Could not find submission ID: " + submissionId));
        boolean piiCompleted = !dataFileRepository
                .existsBySubmissionIdAndPiiPhiFailedIsNullAndFileCategory_CategoryGroup(submissionId, Constants.CATEGORY_DATA);
        ValidationResultsDTO validationResultsDTO = new ValidationResultsDTO();
        validationResultsDTO.setSubmissionId(submissionId);
        validationResultsDTO.setPiiPhiCompleted(piiCompleted);
        if(piiCompleted) {
            List<ValidationResult> bundles = getBundlesValidationResults(submissionId);
            validationResultsDTO.setBundles(bundles);
        }
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
            //if pii validation errors exist, get from db
            if(dataFile.getPiiPhivalidationResults()!=null){
                results.setPiiErrors(mapper.readValue(dataFile.getPiiPhivalidationResults(), Map.class));
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
        //To Be Updated: Start pii job using any file from the submission to provide bucket path to submission prefix
        DataFile dataFile = filesToValidate.get(0);
        CreateClassificationJobResult piiResults = startPIIValidationJob(dataFile, true);
        for (DataFile file : filesToValidate) {
          validateFile(file, file.getFileCategory().getName());
            }
            //trigger step function to check if pii validation is complete after file-specific validation is done
        checkPIIValidationCompleted(piiResults,dataFile,submissionId,true);

        //TODO: Update to return if cde validation, dict validation and meta validation done for their respective files
        // and pii job is started.
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
        String radxProgram = getSubmissionDCC(dataFile.getSubmissionId());
        try {
            performCDEValidation(dataFile, radxProgram);
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
                    String radxProgram = getSubmissionDCC(dataFile.getSubmissionId());
                    performCDEValidation(dataFile, radxProgram);
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
     * @param radxProgram is that dcc associated with the current submission
     */
    private void performCDEValidation(DataFile dataFile, String radxProgram) {
        log.info("ABOUT TO PERFORM CDE VALIDATION FOR: " + dataFile.getSourceFileName());
        S3File s3File = dataFile.getS3File();
        if (s3File == null) {
            throw new DataFileNotFoundException("No S3 file found for ID " + dataFile.getId());
        }

        InputStream object = Objects.requireNonNull(awsStorageService.getS3FileContent(s3File));

        //get s3 file key from path to access file in s3 needed for validation
        CDEValidator cdeValidator = new CDEValidator(dataFile, radxProgram, object);
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
     * startPIIValidationJob finds the associated S3 file, and starts a sensitive discovery job on the file
     * using AWS Macie to scan for pii/phi.
     * @param dataFile    is the file to be validated
     */
    public CreateClassificationJobResult startPIIValidationJob(DataFile dataFile, Boolean submissionLevelScan){
        log.info("ABOUT TO CREATE SENSITIVE DATA SCAN FOR SUBMISSION "+ dataFile.getSubmissionId());
        S3File s3File = dataFile.getS3File();
        if (s3File == null) {
            throw new DataFileNotFoundException("No S3 file found for ID " + dataFile.getId());
        }
        CreateClassificationJobRequest piiAnalysisJobRequest = new CreateClassificationJobRequest();
        String name;
        if(submissionLevelScan){
            name = UUID.randomUUID() + "_" + "test Macie PII Scan for submission: " + dataFile.getSubmissionId();
        }
        else{
            name = UUID.randomUUID() + "_" + "Macie PII Scan for dataFile: " + dataFile.getId();
            //dataFile level scans are only triggered when files are replaced. Make sure pii flags and results are null
            dataFile.setPiiPhiFailed(null);
            dataFile.setPiiPhivalidationResults(null);
            dataFile = dataFileRepository.saveAndFlush(dataFile);
        }
        piiAnalysisJobRequest.setName(name);
        piiAnalysisJobRequest.withSamplingPercentage(100);
        List <String>  managedIdentifiers = List.of("ADDRESS","CREDIT_CARD_NUMBER","NAME","PHONE_NUMBER","VEHICLE_IDENTIFICATION_NUMBER","BANK_ACCOUNT_NUMBER","MEDICAL_DEVICE_UDI","DRIVERS_LICENSE","USA_PASSPORT_NUMBER","USA_SOCIAL_SECURITY_NUMBER","USA_HEALTH_INSURANCE_CLAIM_NUMBER","USA_MEDICARE_BENEFICIARY_IDENTIFIER");
        piiAnalysisJobRequest.setJobType("ONE_TIME");
        piiAnalysisJobRequest.setManagedDataIdentifierIds(managedIdentifiers);
        piiAnalysisJobRequest.setManagedDataIdentifierSelector("INCLUDE");

        //create macie analysis job with the file path set as scope of the scan
        S3JobDefinition s3JobDefinition = new S3JobDefinition();
        configureS3BucketDefinitionForJob(s3JobDefinition,s3File,submissionLevelScan);
        piiAnalysisJobRequest.setS3JobDefinition(s3JobDefinition);
        return macieClient.createClassificationJob(piiAnalysisJobRequest);
    }

    /**
     * checkPIIValidationCompleted creates and triggers step function to track PII scan status
     * Once PII is complete, radx.pii-queue is triggered to populate db with PII scan results
     * @param dataFile    is the file to be validated
     */
    public void checkPIIValidationCompleted(CreateClassificationJobResult piiResults, DataFile dataFile, Integer submissionId, Boolean submissionLevelScan) {
        //if PII scan was not created, exit function
        if(piiResults==null){
            log.info("PII job not created for submission id {} or datafile {}", submissionId, dataFile.getId());
            return;
        }
        dataSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionIdInvalidException("Could not find submission ID: " + submissionId));
        //send job id, datafile id and wait time to step function
        JSONObject sfnInput = new JSONObject();
        sfnInput.put("jobId", piiResults.getJobId());
        sfnInput.put("dataFileId", dataFile.getId());

        //set wait to 30 secs to reduce number of lambda calls to populate PII results.
        sfnInput.put("waitTime", 30);
        sfnInput.put("submissionId", submissionId);
        sfnInput.put("submissionLevelScan", submissionLevelScan);

        //trigger step function
        try {
            String infoMessage;
            String requestName;
            if(submissionLevelScan){
                requestName = UUID.randomUUID() + "_" + "Macie_Scan_For_SubmissionId_" + submissionId;
                infoMessage = " running PII Macie Scan for submissionId " + submissionId;
            }
            else{
                requestName = UUID.randomUUID() + "_" + "Macie_Scan_For_dataFileId_" + dataFile.getId();
                infoMessage = " running PII Macie Scan for dataFileId " + dataFile.getId();
            }

            StartExecutionRequest executionRequest = StartExecutionRequest.builder()
                    .input(sfnInput.toString())
                    .stateMachineArn(machineArn)
                    .name(requestName)
                    .build();
            StartExecutionResponse response = sfnClient.startExecution(executionRequest);
            if (response.sdkHttpResponse().isSuccessful()) {
                log.info("triggered step function--{}_{}" , response.sdkHttpResponse().statusCode() , infoMessage );
            }

        } catch (SfnException e) {
            log.info("Error occurred when triggering or processing step function for PII validation: {} ", e.awsErrorDetails().errorMessage());
            throw new ValidationErrorException("Error occurred when triggering or processing step function for PII validation: " + e.awsErrorDetails().errorMessage());
        }
    }

    /**
     * radx.pii-queue listener responds to completion of the step function once the file scan is complete
     * processMacieScanResults responds to sqs queue that is triggered once sensitive data discovery job is finished for the file
     * and gets the pii validation results from Macie once sensitive data discovery job is complete
     * and updates the file metadata and the database as needed
     * @param message is the message received from sqs
     */
    @SqsListener(value = "${radx.pii-queue}")
    public void processMacieScanResults(Message message){
        log.info("Messaged received to trigger processing macie scan results : ..." + message.body());
        JsonNode piiFileDetails = null;
        try {
            piiFileDetails = mapper.readTree(message.body());
        } catch (JsonProcessingException e) {
            log.info("Unable to process messaged received from PII SQS Queue: "+ e.getMessage());
            throw new ValidationErrorException("Unable to process messaged received from PII SQS Queue: "+ e.getMessage());
        }
        String jobId = piiFileDetails.get("jobId").asText();
        Integer submissionId = piiFileDetails.get("submissionId").asInt();
        Boolean submissionLevelScan = piiFileDetails.get("submissionLevelScan").asBoolean();
        Integer dataFileId = piiFileDetails.get("dataFileId").asInt();
        log.info("Message details to process macie scan results : ..." + piiFileDetails.toString());
        getMacieScanFindings(jobId, submissionId,submissionLevelScan, dataFileId);

    }

    /**
     * getMacieScanFindings gets the occurrences of sensitive data from the findings information and converts the information
     * into ValidationError objects
     * @param jobId is the jobId for the sensitive data discovery job
     * @return errors which is a list of the validation errors based on findings
     */

    private  void getMacieScanFindings(String jobId, Integer submissionId, Boolean submissionLevelScan, Integer dataFileId){
        log.info("ABOUT TO GET MACIE SCAN FINDINGS FOR SUBMISSION ID: " +submissionId+ " AND JOB ID "+ jobId);
        ListFindingsRequest listFindingsRequest = new ListFindingsRequest();
        FindingCriteria findingCriteria = new FindingCriteria();
        CriterionAdditionalProperties criterionAdditionalProperties = new CriterionAdditionalProperties();
        criterionAdditionalProperties.withEq(jobId);
        findingCriteria.addCriterionEntry("classificationDetails.jobId",criterionAdditionalProperties);
        listFindingsRequest.withFindingCriteria(findingCriteria);

        ListFindingsResult listFindingsResult = macieClient.listFindings(listFindingsRequest);
        List <String> findings =  listFindingsResult.getFindingIds();

        if(!findings.isEmpty()){
            GetFindingsResult getFindingsResult = macieClient.getFindings(new GetFindingsRequest().withFindingIds(findings));
            List <Finding> macieScanFindings = getFindingsResult.getFindings();
            //if findings exists process the results
            if (!macieScanFindings.isEmpty()){
                log.info("FOUND SENSITIVE MACIE SCAN FINDINGS FOR SUBMISSION ID: " +submissionId+ "JOB ID "+ jobId);

                //get the detailed results location from a macie finding in order to have the bucket details where the results are stored
                //for this job
                String detailedResults = macieScanFindings.get(0).getClassificationDetails().getDetailedResultsLocation();
                List<JsonNode> jsonResults =  awsStorageService.getGZipS3FileContent(detailedResults.replace("s3://",""));
                jsonResults.forEach(jsonNode -> processJsonResult(jsonNode));
            }
        }
        else{
            //if there are no findings, the datafile(s) passed pii validation
            if(submissionLevelScan){
                boolean piiCompleted = dataFileRepository
                        .existsBySubmissionIdAndPiiPhiFailedIsNullAndFileCategory_CategoryGroup(submissionId, Constants.CATEGORY_DATA);
                List<DataFile> dfsPassedPII = dataFileRepository.findDataFilesByFileCategory_CategoryGroupAndSubmissionIdAndPiiPhiFailedIsNull(Constants.CATEGORY_DATA, submissionId);
                dfsPassedPII.forEach(dataFile -> dataFile.setPiiPhiFailed(false));
                dataFileRepository.saveAll(dfsPassedPII);
            }else{
                DataFile dataFile = dataFileRepository.findById(dataFileId).get();
                dataFile.setPiiPhiFailed(false);
                dataFileRepository.save(dataFile);
            }
        }
    }

    /**
     * processJsonResult finds the associated dataFile and update it's PII validation errors
     * It is triggered when macie findings exist for scan
     * @param jsonResults
     */
    @Transactional
    public void processJsonResult(JsonNode jsonResults){
        String s3FilePath =  jsonResults.get("resourcesAffected").get("s3Object").get("path").asText();
        //find s3 file by path
        S3File s3File = s3FileRepository.findS3FileByFilePath(s3FilePath);
        DataFile dataFile = dataFileRepository.findDataFileByS3FileId(s3File.getId());
        //only if it is a datafile, save pii failed as true of false, otherwise null
        if(dataFile.getFileCategory().getCategoryGroup().equals(Constants.CATEGORY_DATA)){
            //for each line in the zipfile do this.
            List<ValidationError> scanResults = new ArrayList<>();
            MultiValuedMap<String, ValidationError> errors = new ArrayListValuedHashMap<>();
            var sensitiveData = jsonResults.get("classificationDetails").get("result").get("sensitiveData");
            dataFile.setPiiPhiFailed(!sensitiveData.isEmpty());
            if(!sensitiveData.isEmpty()){
                Integer totalPIICount = sensitiveData.get(0).get("totalCount").asInt();
                try {
                    ValidationResult results = null;
                    if(dataFile.getValidationResults()!=null){
                        results =  mapper.readValue(dataFile.getValidationResults(), ValidationResult.class);
                        var currentWarningCount = results.getDataEntryWarningCount();
                        if (currentWarningCount != null){
                            var totalWarnings = currentWarningCount.intValue() + totalPIICount;
                            results.setDataEntryWarningCount(totalWarnings);
                            dataFile.setValidationResults(mapper.writeValueAsString(results));
                        }
                    }
                    else{
                        results = new ValidationResult(dataFile);
                        results.setDataEntryWarningCount(totalPIICount);
                        dataFile.setValidationResults(mapper.writeValueAsString(results));
                    }
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                String message = jsonResults.get("description").asText();
                String solution = "Please reference our De-Identification Guidance to correctly de-identify data as defined in RADx Requirements.";
                var details = sensitiveData.get(0).get("detections");
                details.forEach(jsonNode -> {
                    var cells = jsonNode.get("occurrences").get("cells");
                    cells.forEach(cellNode -> scanResults.add(new ValidationError("PERSONAL_INFORMATION",null,jsonNode.get("type").asText(),
                            cellNode.get("columnName").asText(),cellNode.get("row").asLong(),message,solution)));
                });
                if(!scanResults.isEmpty()){
                    scanResults.forEach(validationError -> errors.put(validationError.getErrorType(), validationError));
                    try {
                        //set file's  pii validation results
                        dataFile.setPiiPhivalidationResults(mapper.writeValueAsString(errors.asMap()));
                    } catch (JsonProcessingException e) {
                        throw new ValidationErrorException("Unable to Update PII Validation Results in Database: " + e.getMessage());
                    }
                }
            }
            dataFileRepository.saveAndFlush(dataFile);
        }
    }

    /**
     * configureS3BucketDefinitionForJob sets scope of the file to analyse
     * @param s3JobDefinition defines the scope of the file
     * @param s3File
     */
    private void configureS3BucketDefinitionForJob(S3JobDefinition s3JobDefinition, S3File s3File, Boolean submissionLevelScan) {
        S3BucketDefinitionForJob s3BucketDefinitionForJob = new S3BucketDefinitionForJob();
        s3File.setS3FileKeyAndBucketFromPath();
        s3BucketDefinitionForJob.withBuckets(s3File.getFileBucket());
        s3BucketDefinitionForJob.setAccountId(accountNumber);
        Scoping scoping = getScoping(s3File, submissionLevelScan);
        s3JobDefinition.setScoping(scoping);
        s3JobDefinition.withBucketDefinitions(s3BucketDefinitionForJob);
    }

    /**
     * getScoping defines the scope for the classification job to run on file level
     * @param s3File is the file being scanned
     */
    private static Scoping getScoping(S3File s3File, Boolean submissionLevelScan) {
        Scoping scoping = new Scoping();
        JobScopingBlock jobScopingBlock= new  JobScopingBlock();
        SimpleScopeTerm fileScope = new SimpleScopeTerm();
        //scope scan to file
        fileScope.setComparator("STARTS_WITH");
        fileScope.setKey("OBJECT_KEY");
        if(submissionLevelScan){
            String submissionPath = s3File.getFileKey().replace(s3File.getFileName(),"");
            fileScope.withValues(submissionPath);
        }else{
            fileScope.withValues(s3File.getFileKey());
        }
        JobScopeTerm fileScopeTerm = new JobScopeTerm();
        fileScopeTerm.setSimpleScopeTerm(fileScope);

        SimpleScopeTerm fileExtensionScope = new SimpleScopeTerm();
        fileExtensionScope.setComparator("EQ");
        fileExtensionScope.setKey("OBJECT_EXTENSION");
        fileExtensionScope.withValues("csv");
        JobScopeTerm fileExtensionScopeTerm = new JobScopeTerm();
        fileExtensionScopeTerm.setSimpleScopeTerm(fileExtensionScope);

        List<JobScopeTerm>jobScopeTerms = new ArrayList<>();
        jobScopeTerms.add(fileScopeTerm);
        jobScopeTerms.add(fileExtensionScopeTerm);

        jobScopingBlock.withAnd(jobScopeTerms);
        scoping.setIncludes(jobScopingBlock);
        return scoping;
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
     * getSubmissionDCC gets the RADx DCC associated with submission
     *
     * @param submissionId is the submissionId
     * @return dcc
     */
    private String getSubmissionDCC(Integer submissionId) {
        DataSubmission dataSubmission = dataSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionIdInvalidException("Could not find submission ID: " + submissionId));
        Study study = studyRepository.findStudyById(dataSubmission.getStudyId());
        Optional<LkupDCC> lkupDCC = lkupDCCRepository.findById(Math.toIntExact(study.getDcc().getId()));
        String dcc = lkupDCC.get().getName();
        if (dcc.isEmpty()) {
            throw new BadDataException("Valid DCC not found");
        }
        return dcc;
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
				|| (df.getDictValidationFailed() != null && df.getDictValidationFailed())
				|| df.getPiiPhiFailed() != null && df.getPiiPhiFailed();
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
            case "pii" -> validationResult.setPiiErrors(errors);
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



