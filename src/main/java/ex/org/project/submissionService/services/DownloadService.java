package ex.org.project.submissionService.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import ex.org.project.submissionService.exceptions.custom.BadDataException;
import ex.org.project.submissionService.exceptions.custom.SubmissionIdInvalidException;
import ex.org.project.submissionService.models.DataFile;
import ex.org.project.submissionService.models.ValidationError;
import ex.org.project.submissionService.models.ValidationResult;
import ex.org.project.submissionService.repositories.DataFileRepository;
import ex.org.project.submissionService.repositories.DataSubmissionRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class DownloadService {

    private final DataFileRepository dataFileRepository;
    private final DataSubmissionRepository dataSubmissionRepository;
    private static final String[] CSV_HEADER = {"Validation Type", "Error Type", "File", "File Type", "Line Number", "Column Header", "Value", "Message", "Solution"};


    /**
     * This method generates a CSV file containing validation errors for a given fileId
     *
     * @param response The HttpServletResponse object to write the CSV file to
     * @param fileId   The ID of the file to retrieve the validation errors for
     */
    public void getValidationErrors(HttpServletResponse response, Integer fileId) {
        Optional<DataFile> dataFile = dataFileRepository.findById(fileId);
        // If the DataFile object exists, proceed with generating the CSV file
        dataFile.ifPresent(df -> {
            try {
                ValidationResult validationResult = getDataFileErrors(df);
                StringWriter stringWriter = new StringWriter();
                CSVWriter csvWriter = new CSVWriter(stringWriter);
                csvWriter.writeNext(CSV_HEADER);
                List<String[]> allLines = new ArrayList<>();
                addValidationErrorsToCSV(validationResult, allLines);
                if(!allLines.isEmpty()){
                    csvWriter.writeAll(allLines);
                }
                String csvData = stringWriter.toString();
                response.setContentType("text/csv");
                response.setHeader("Content-Disposition", "attachment; filename=validation_errors.csv");
                response.getWriter().write(csvData);
                csvWriter.close();

            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                log.error("Exception", e);
            }
        });
    }

    /**
     * This method retrieves the validation errors for a given file that will populate the CSV
     *
     * @param df  The file to retrieve the validation errors for
     */
    public ValidationResult getDataFileErrors(DataFile df){
        ValidationResult validationResult;
        try {
            validationResult = new ObjectMapper().readValue(df.getValidationResults(), ValidationResult.class);
        } catch (JsonProcessingException e) {
            throw new BadDataException("Unable to parse validation results for datafile: " + df.getId() + "from database");
        }
        if(df.getPiiPhivalidationResults()!=null){
            MultiValuedMap<String, ValidationError> errorsMap = new ArrayListValuedHashMap<>();
            Map errors = null;
            try {
                errors = new ObjectMapper().readValue(df.getPiiPhivalidationResults(), Map.class);
            } catch (JsonProcessingException e) {
                throw new BadDataException("Unable to parse PII validation results for datafile: " + df.getId() + "from database");
            }
            for (Object key : errors.keySet()){
                //convert the validation results to Validation errors
                List<ValidationError> validationErrors = new ObjectMapper() .convertValue(errors.get(key), new TypeReference<List<ValidationError>>() { });
                for (ValidationError validationError: validationErrors){
                    errorsMap.put(key.toString(),validationError);
                }
            }
            validationResult.setPiiErrors(errorsMap.asMap());
        }
        return validationResult;
    }


    /**
     * This method adds validation errors to a CSV file.
     * It takes a map of validation errors, a list of all lines in the CSV file, and a validation result object.
     * It iterates over the validation errors, extracts relevant information, and adds it to the appropriate positions in each line.
     * Finally, it adds the updated lines to the list of all lines.
     */

    public void addValidationErrors(Map<String, Collection<ValidationError>> validationErrors, List<String[]> allLines, ValidationResult validationResult) {
        for (Collection<ValidationError> errors : validationErrors.values()) {

            for (ValidationError error : errors) {
                String[] line = new String[10];
                line[2] = String.valueOf(validationResult.getFileName());
                line[3] = String.valueOf(validationResult.getFileType());
                line[0] = error.getValidationType();
                line[6] = error.getValue();
                line[7] = error.getMessage();
                line[8] = error.getSolution();
                line[1] = error.getErrorType();
                line[4] = String.valueOf(error.getLineNumber());
                line[5] = error.getColumnHeader();
                allLines.add(line);
            }
        }
    }

    /**
     * Gets the validation errors for a submission and generates a CSV report.
     *
     * @param response     The HttpServletResponse object to send the CSV report as a response.
     * @param submissionId The ID of the submission for which to retrieve validation errors.
     */
    public void getValidationErrorsbySubmission(HttpServletResponse response, Integer submissionId) {
        try {
            //check if data submission id is valid
            dataSubmissionRepository.findById(submissionId)
                    .orElseThrow(() -> new SubmissionIdInvalidException("Could not find submission ID: " + submissionId));
            // Retrieve the data files associated with the submission
            List<DataFile> dataFiles = dataFileRepository.findBySubmissionId(submissionId);

            // Create a CSV writer and add headers
            StringWriter stringWriter = new StringWriter();
            CSVWriter csvWriter = new CSVWriter(stringWriter);
            csvWriter.writeNext(CSV_HEADER);

            List<String[]> allLines = new ArrayList<>();
            // Iterate over each data file and generate a CSV report if validation results are available
            for (DataFile dataFile : dataFiles) {
                if (dataFile.getValidationResults() != null) {
                    // Parse the validation results from JSON to ValidationResult object
                    ValidationResult validationResult = getDataFileErrors(dataFile);
                    // Generate a CSV report and add rows
                    addValidationErrorsToCSV(validationResult, allLines);
                }
            }

            if(!allLines.isEmpty()){
                csvWriter.writeAll(allLines);
            }
            String csvData = stringWriter.toString();
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=validation_errors.csv");
            response.getWriter().write(csvData);
            csvWriter.close();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.error("Exception", e);
        }
    }
    /**
     *This method takes a ValidationResult object and a CSVWriter object as input parameters
     * @param validationResult
     * @param allLines
     */

    public void addValidationErrorsToCSV(ValidationResult validationResult, List<String[]> allLines) throws IOException {

        extractErrors(validationResult.getPiiErrors(), allLines, validationResult);
        extractErrors(validationResult.getCdeErrors(), allLines, validationResult);
        extractErrors(validationResult.getMetaErrors(), allLines, validationResult);
        extractErrors(validationResult.getDictErrors(), allLines, validationResult);
    }

    private void extractErrors(Map<String, Collection<ValidationError>> validationErrors, List<String[]> allLines,
                               ValidationResult validationResult) {
        Optional.ofNullable(validationErrors)
                .filter(errors -> !errors.isEmpty())
                .ifPresent(errors -> addValidationErrors(errors, allLines, validationResult));
    }

}
