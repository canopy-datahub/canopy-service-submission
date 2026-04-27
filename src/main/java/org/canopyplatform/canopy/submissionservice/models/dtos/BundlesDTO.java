package org.canopyplatform.canopy.submissionservice.models.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BundlesDTO {
    private Integer id;
    private String sourceFileName;
    private Integer versionNumber;
    private String fileCategory;
    private Boolean piiPhiFailed;
    private Boolean cdeFailed;
    private Boolean metaFailed;
    private Boolean dictFailed;
    private String reviewDecision;

}
