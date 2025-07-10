package ex.org.project.submissionService.models.dtos;

import java.util.List;

public record GetBundleFilesDTO(Boolean isLastBundle, List<BundleFileDTO> files) {}
