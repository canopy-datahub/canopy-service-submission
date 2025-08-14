package ex.org.project.submissionService.models.dtos;

import ex.org.project.submissionService.models.ValidationResult;
import lombok.Data;

import java.util.List;

@Data
public class ValidationResultsDTO {
    private int submissionId;
    private List<ValidationResult> bundles;
}
