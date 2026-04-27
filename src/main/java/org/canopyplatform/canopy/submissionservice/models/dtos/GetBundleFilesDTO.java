package org.canopyplatform.canopy.submissionservice.models.dtos;

import java.util.List;

public record GetBundleFilesDTO(Boolean isLastBundle, List<BundleFileDTO> files) {}
