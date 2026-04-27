package org.canopyplatform.canopy.submissionservice.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
public class ValidationResult {
    private Integer fileId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Boolean acknowledged;
    private String validationType;
    private String validationResultsStatus;
    private HashSet<String> missingHeaders;
    private Integer dataEntryWarningCount;
    private Map<String, Collection<ValidationError>> cdeErrors;
    private Map<String, Collection<ValidationError>> dictErrors;
    private Map<String, Collection<ValidationError>> metaErrors;
    private List<ValidationResult> childFiles;

    public ValidationResult(DataFile dataFile){
        this.fileId = dataFile.getId();
        this.fileName = dataFile.getSourceFileName();
        this.fileType = dataFile.getFileCategory().getName();
        this.fileSize = dataFile.getFileSize();
        this.childFiles = new ArrayList<>();
    }
}
