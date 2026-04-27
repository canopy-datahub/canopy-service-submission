package org.canopyplatform.canopy.submissionservice.models.dtos;

import org.canopyplatform.canopy.submissionservice.models.ValidationResult;
import lombok.Data;

import java.util.List;

@Data
public class ValidationResultsDTO {
    private int submissionId;
    private List<ValidationResult> bundles;
}
