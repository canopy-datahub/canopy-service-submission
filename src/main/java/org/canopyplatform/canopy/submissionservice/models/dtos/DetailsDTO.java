package org.canopyplatform.canopy.submissionservice.models.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DetailsDTO {
    private Integer submissionId;
    private String studyName;
    private String centerRep;
    private String center;
    private List<BundlesDTO> bundles;
}
