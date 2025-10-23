package ex.org.project.submissionService.services;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.CSVReaderHeaderAwareBuilder;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.exceptions.CsvException;
import ex.org.project.submissionService.exceptions.custom.CodebookNotFoundException;
import ex.org.project.submissionService.exceptions.custom.ValidationErrorException;
import ex.org.project.submissionService.models.DataFile;
import ex.org.project.submissionService.models.ValidationError;
import ex.org.project.submissionService.models.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
@Slf4j
public class CDEValidator {

    private MultiValuedMap<String, ValidationError> errors;
    private final ValidationResult validationResult;
    private HashMap<String, String> validResponsesMap;
    private HashMap<String, String> globalValidResponsesMap;
    private HashSet<String> fileTier1Headers;
    private final InputStream object;
    private final DataFile dataFile;
    private Integer totalWarnings = 0;
    private List<String> header;
    private HashSet<String> missingHeaders;

    private final HashSet<String> invalidZtca = new HashSet<String>(Arrays.asList("036", "059", " 063", "102", "203", "556", "692", "790", "821", "823", "830", "831", "878", "879",
            "884", "890", "893"));

    public CDEValidator(DataFile dataFile, InputStream object) {
        this.object = Objects.requireNonNull(object);
        this.dataFile = Objects.requireNonNull(dataFile);
        this.validationResult = new ValidationResult(dataFile);
        this.missingHeaders = new HashSet<>();
        performCDEValidation();
    }

    /**
     * performCDEValidation checks if the file has Tier1 variables and valid record count, if it does file validation occurs
     * if no tier1 variables are found, that is noted in validation dto
     */
    private void performCDEValidation() {
        readCodebook();
        errors = new ArrayListValuedHashMap<>();
        int parserSize = parseAndValidateCSV();
        //check if records number is greater than or equal to 1
        if (parserSize == 0) {
            updateValidationDTO("Warning: CSV Records does not have at least 1 row of data apart from header");
            errors.put("Invalid Record Count", new ValidationError("CDE Validation", null,"Invalid Record Count", null, null,
                    "Warning: Found " + parserSize + " record entries. CSV Records should have at least 2 rows of data" , "Please ensure CSV files have at least 2 rows of data"));
            totalWarnings++;
            dataFile.setCdeValidationFailed(true);
        }
    }

    /**
     * Read RADx_Global_Codebook and collect valid tier1 headers and responses for specific program
     */
    private void readCodebook() {

        InputStream codebook = getCodebookFromResourceAsStream();
        XSSFWorkbook workbook;
        try {
            workbook = new XSSFWorkbook(codebook);
        } catch (IOException e) {
            throw new CodebookNotFoundException("Unable to read codebook");
        }

        XSSFSheet globalprogramValues = workbook.getSheet("2_RADx_Global_Codebook");

        //populate map with program --> {valid CDE header: valid responses string}
        globalValidResponsesMap = getValidResponses(globalprogramValues, "global");
    }

    /**
     * Gets RADx_Global_Codebook from the resources folder
     */
    private InputStream getCodebookFromResourceAsStream() {
        String fileName = "Global_Codebook_Final.xlsx";
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
        // the stream holding the file content
        if (inputStream != null) {
            return inputStream;
        } else {
            throw new IllegalArgumentException("Global Codebook not found! " + fileName);
        }
    }

    /**
     * Checks if file has tier 1 headers relating to it's program
     *
     * @param fileHeaders is the file object.
     * @return true if it has tier 1 variables, false if no tier 1 variables exist in file
     */
    private Boolean hasTier1Variable(HashSet<String> fileHeaders) {
        if (validResponsesMap == null) {
            readCodebook();
        }
        //get all dcc required tier 1 cde headers for the specific program
        HashSet<String> programTier1 = new HashSet<>(validResponsesMap.keySet());
        //collect the file's tier 1 headers
        fileTier1Headers = new HashSet<>(CollectionUtils.intersection(programTier1, fileHeaders));

        //collect the file's missing headers
        missingHeaders = new HashSet<>(CollectionUtils.disjunction(programTier1, fileTier1Headers));
        validationResult.setMissingHeaders(missingHeaders);
        errors.put("missingHeaders", new ValidationError("CDE Validation", missingHeaders.toString(),"Missing Tier1 Headers", null,null,  "File missing required Headers",
                "update file headers"));

        //If the file does not have any valid tier 1 cdes then is it not a file valid for cde validation
        return !fileTier1Headers.isEmpty();
    }

    /**
     * Collects the valid program ---->  {cdeHeader:valid responses string} pairing for program based on codebook sheet
     * Cell(2) refers to the Global Variable column in codebook
     * Cell(3) refers to the Global Response Options column in codebook
     * @param sheet is the sheet in codebook relating to specific program.
     * @return validProgramVals, a hashMap with tier1 header as key and of valid responses + label string as respective value
     */
    private HashMap<String, String> getValidResponses(XSSFSheet sheet, String parseType) {
        if (sheet == null) {
            readCodebook();
        }
        HashMap<String, String> validProgramVals = new HashMap<>();
        assert sheet != null;
        Integer requiredCellLoc = 4;
        Integer keyCellLoc = 2;
        Integer valueCellLoc = 3;

        if(parseType.equals("global")){
            requiredCellLoc = 1;
            keyCellLoc = 2;
            valueCellLoc = 3;
        }
        for (Row r : sheet) {
            //skip header
            if (r.getRowNum() == 0) {
                r.setRowNum(1);
                continue;
            }
            if(r.getCell(requiredCellLoc)!=null){
                String variableCell = r.getCell(requiredCellLoc).getStringCellValue();
                //if the value in the DCC Program Variable column (cell 4) is not blank, add the key and value to map
                if (!variableCell.isEmpty()) {
                    //Global Variable
                    String keyCell = r.getCell(keyCellLoc).getStringCellValue();
                    //Global Response Options
                    String valCell = r.getCell(valueCellLoc).getStringCellValue();
                    validProgramVals.put(keyCell, valCell);
                }
            }
        }
        return validProgramVals;
    }

    /**
     * Parses the valid responses string to get response values
     * @param validResponse is string of response options for each tier1 variable,
     *                      concatenated of pairs of "validValue,validValueDescription;validValue2,validValueDescription2..."
     * @return validResponses, a hashSet of valid response values
     */
    private HashSet<String> parseValidResponsesString(String validResponse) {
        if (validResponse.isEmpty()) {
            readCodebook();
        }
        HashSet<String> validResponses = new HashSet<>();
        for (String keyValue : validResponse.split(" *; *")) {
            String[] key = keyValue.split(" *, *");
            if (key.length == 2) {
                validResponses.add(key[0]);
            } else if (key.length == 1) {
                continue;
            }
        }
        return validResponses;

    }

    /**
     * parseAndValidateCSV  parses the csv file header, collects the header values and the file records, the do validation.
     * returns record count
     */
    private int parseAndValidateCSV()
    {
        var csvRecordCount = 0;
        try (var br = new InputStreamReader(object, StandardCharsets.UTF_8)){
            RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder().build();

            CSVReaderHeaderAware csvReader = new CSVReaderHeaderAwareBuilder(br)
                    .withCSVParser(rfc4180Parser)
                    .build();

            Map<String, String> values = csvReader.readMap();
            //if there are no headers or empty headers, this should be a validation warning
            if (values == null || values.keySet().isEmpty() || values.keySet().stream().anyMatch(s -> s.isEmpty())) {
                updateValidationDTO("Validation Has Warnings");
                errors.put("Invalid CSV File", new ValidationError("CDE Validation", null, "Empty Column Header Value(s) Found", null, null,
                        "Column Header Is Either Empty or Have Missing Values: "+ header + " Please replace with valid file", "Please ensure CSV files is valid"));
                totalWarnings++;
            }
            else{
                //if header is valid
                header = values.keySet().stream().toList();
                boolean hasCoreHeaders = hasTier1Variable(new HashSet<>(header));
                //updates the dataFile's FileHeaders value
                setCsvHeader();
                //having no cde headers is a warning
                if (!hasCoreHeaders) {
                    updateValidationDTO("Warning: No Valid Tier1 Headers to Validate");
                    totalWarnings++;
                    dataFile.setCdeValidationFailed(true);
                    //read all records to get count, no need for validation checks
                    csvReader.readAll();
                }
                else{
                    //if it has core variables that need cde checks
                    //for each file records, validate values under cde columns
                    //already read header, so line number starts at 1
                    int line = 1;
                    while (((values = csvReader.readMap())) != null){
                        for (String header : fileTier1Headers) {
                            var value = values.get(header);
                            validateValues(header, errors, value, line);
                        }
                        line++;
                        }
                }
                csvRecordCount = (int) csvReader.getRecordsRead();
                dataFile.setVariablesCount(header.size());
                dataFile.setSampleSize(csvRecordCount -1); //set sample size all records excluding header
            }
        }
        catch (IOException e) {
            updateValidationDTO("Validation Has Warnings");
            errors.put("Invalid CSV File", new ValidationError("CDE Validation", null, "CSV Reader Error", null, null,
                    e.getMessage(), "Please ensure CSV files is valid"));
        } catch (CsvException e) {
            log.error("CsvException: Failed to parse CSV records for file "+ dataFile.getId() + ": " + e.getMessage());
            throw new ValidationErrorException("CsvException: Failed to parse CSV records for file "+ dataFile.getId() + ": " + e.getMessage());
        }
        catch (Exception e){
            log.error("CDE Validation Failed for file "+ dataFile.getId() + ": " + e.getMessage());
            throw new ValidationErrorException("CDE Validation Failed for file "+ dataFile.getId() + ": " + e.getMessage());
        }
        return csvRecordCount;
    }

        /**
     * parseCsvHeader gets the csv file header and updates the dataFile's FileHeaders value
     */
    private HashSet<String> setCsvHeader() {
        HashSet distinctHeader = null;
        if (!header.isEmpty()) {
            String fileHeader = String.join(";", header);
            dataFile.setFileHeaders(fileHeader);
            distinctHeader = new HashSet<>(header);
            if (header.size() != distinctHeader.size()) {
                updateValidationDTO("Validation Has Warnings");
                errors.put("Invalid CSV File", new ValidationError("CDE Validation", null,"CSV Reader Error", null, null,
                        " Input CSV File Has Repeated headers: "+  new HashSet<>(CollectionUtils.disjunction(header, distinctHeader)).toString()  , "Please ensure CSV files is valid"));
            }
        }
        return distinctHeader;
    }

    /**
     * validateValues loops through each  cde column in file and checks if each value passes validation based on custom validation rules
     * @param header tier1 variable header
     * @param errors is a map of collecting validation errors
     *
     */
    private void validateValues(String header, MultiValuedMap<String, ValidationError> errors, String val, long line) {
        try {
            //get the set of valid values for the specific cde header.
            String validResponsesStr = globalValidResponsesMap.get(header);
            HashSet<String> headerValidValues = parseValidResponsesString(validResponsesStr);

            HashSet<String> possibleZtcaLengthTwo = new HashSet<String>(Arrays.asList("36", "59", " 63"));
            int warningCount = 0;

                switch (header) {
                    case "nih_record_id" -> {
                        //For "nih_record_id" entries, the only requirement is that it is a unique string of at least size 1
                        if (val.isEmpty()) {
                            String solution = "For nih_record_id entries, the only requirement is that it is a unique string of at least size 1";
                            errors.put(header, new ValidationError("CDE Validation", val, "UnexpectedValue",header, line, "Invalid value found", solution));
                            warningCount++;
                        }
                    }

                    //valid nih_education_yrs is 0-50 or valid value
                    case "nih_education_yrs" -> {
                        boolean ruleSet = val != null && !val.isEmpty() && ((Integer.parseInt(val) >= 0 && Integer.parseInt(val) <= 50) || headerValidValues.contains(val));
                        if (!ruleSet) {
                            String solution = "valid nih_education_yrs is 0-50 or valid values: " + validResponsesStr;
                            errors.put(header, new ValidationError("CDE Validation", val, "UnexpectedValue", header, line, "Invalid value found", solution));
                            warningCount++;
                        }
                    }

                    //valid nih-age values are string values 0 to 90 inclusively or valid missing data codes
                    case "nih_age" -> {
                        boolean ruleSet = val != null && !val.isEmpty() && ((Integer.parseInt(val) >= 0 && Integer.parseInt(val) <= 90) || headerValidValues.contains(val));
                        if (!ruleSet) {
                            String solution = "valid valid nih-age values are string values between 0 and 90 or valid values: " + validResponsesStr;
                            errors.put(header, new ValidationError("CDE Validation", val, "UnexpectedValue", header, line, "Invalid value found", solution));
                            warningCount++;
                        }
                    }
                    //nih-zip are string values Must be a 3 digit number and if the first three digits are in a ZCTA invalid list is in incorrect
                    case "nih_zip" -> {
                        if (possibleZtcaLengthTwo.contains(val)) {
                            val = '0' + val;
                        }
                        boolean isDigits = val != null && !val.isEmpty() && val.chars().allMatch(Character::isDigit);
                        boolean ruleSet = (isDigits && !invalidZtca.contains(val) && val.length() == 3) || headerValidValues.contains(val);
                        if (invalidZtca.contains(val)) {
                            String solution = "valid nih-zip are string values must be a 3 digits and not be in ZCTA list: " + invalidZtca;
                            errors.put(header, new ValidationError("CDE Validation", val, "RestrictedZipFound", header, line, "Invalid value found", solution));
                            warningCount++;
                        } else if (!ruleSet) {
                            String solution = "valid nih-zip are string values must be a of length 3 and not in ZCTA list: " + invalidZtca + "or be a valid values:" + validResponsesStr;
                            errors.put(header, new ValidationError("CDE Validation", val, "UnexpectedValue", header, line, "Invalid value found", solution));
                            warningCount++;
                        }
                    }

                    //the values nih-weight are integer values between 0 and 2000 or valid missing data codes as defines in the global codebook
                    case "nih_weight" -> {
                        boolean ruleSet = val != null && !val.isEmpty() && ((Integer.parseInt(val) > 0 && Integer.parseInt(val) <= 2000) || headerValidValues.contains(val));
                        if (!ruleSet) {
                            String solution = "valid nih-weight values range between 0 and 2000 or valid values:" + validResponsesStr;
                            errors.put(header, new ValidationError("CDE Validation", val, "UnexpectedValue", header, line, "Invalid value found", solution));
                            warningCount++;
                        }
                    }

                    //the values nih-height are integer values between 0 and 120 or valid missing data codes as defines in the global codebook
                    case "nih_height" -> {
                        boolean ruleSet = val != null && !val.isEmpty() && ((Integer.parseInt(val) > 0 && Integer.parseInt(val) <= 150) || headerValidValues.contains(val));
                        if (!ruleSet) {
                            String solution = "valid nih-height are integer values between 0 and 120 or valid values:" + validResponsesStr;
                            errors.put(header, new ValidationError("CDE Validation", val, "UnexpectedValue", header, line, "Invalid value found", solution));
                            warningCount++;
                        }
                    }

                    //all other valid values are strictly defined in the codebook
                    default -> {
                        boolean ruleSet = val != null && !val.isEmpty() && headerValidValues.contains(val);
                        if (!ruleSet) {
                            String solution = " valid " + header + " values include: " + validResponsesStr;
                            errors.put(header, new ValidationError("CDE Validation", val, "UnexpectedValue", header, line, "Invalid value found", solution));
                            warningCount++;
                        }
                    }
                }
            totalWarnings += warningCount;

        //handle when decimals are uploaded, expected format is integer only
        } catch(NumberFormatException e) {
            String solution = "Invalid format " + e.getMessage()  +" found for " + header + ". Valid values should be integers.";
            log.error(solution);
            throw new ValidationErrorException( solution);

        }catch (Exception e) {
            log.error("Unable to perform cde validation on files: " + e.getMessage());
            throw new ValidationErrorException("Unable to perform cde validation on files: " + e.getMessage());
        }
    }

    /**
     * getCDEValidationResult updates the status and values of the validation results and
     * returns a json string that represents the validation results
     * @return json string representation of validation result.
     */
    public ValidationResult getCDEValidationResult() {
        String status;

        if (missingHeaders.isEmpty() && totalWarnings == 0) {
            status = "Validation Passed";
            dataFile.setCdeValidationFailed(false);
        } else {
            status = "Validation Has Warnings";
            dataFile.setCdeValidationFailed(true);
            if (!errors.isEmpty()) {
                validationResult.setCdeErrors(errors.asMap());
                validationResult.setDataEntryWarningCount(totalWarnings);
            }
        }
        updateValidationDTO(status);
        return validationResult;

    }

    /**
     * updateValidationDTO updates the validation DTO with status and validation type
     * @param status is the status of the validation results
     */
    private void updateValidationDTO(String status) {
        validationResult.setValidationType("Tier1 Headers Ruleset Validation Result");
        //updates status message only if custom status is not already set
        if(validationResult.getValidationResultsStatus()==null){
            validationResult.setValidationResultsStatus(status);
        }
    }


}
