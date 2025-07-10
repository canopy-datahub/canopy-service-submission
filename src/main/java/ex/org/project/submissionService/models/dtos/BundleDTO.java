package ex.org.project.submissionService.models.dtos;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class BundleDTO {

    private Integer id;
    private String name;
    private String category;
    private Long size;
    private Boolean cdeValidation;
    private Boolean acknowledged;
    private Boolean piiPhiValidation;
    private Boolean willBeVersioned;

    private List<BundleDTO> childFiles;

    public BundleDTO(){
        this.childFiles = new ArrayList<>();
    }
}
