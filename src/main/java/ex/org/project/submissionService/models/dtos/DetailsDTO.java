package ex.org.project.submissionService.models.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DetailsDTO {
    private Integer submissionId;
    private String studyName;
    private String phs;
    private String centerRep;
    private String center;
    private List<BundlesDTO> bundles;
}
