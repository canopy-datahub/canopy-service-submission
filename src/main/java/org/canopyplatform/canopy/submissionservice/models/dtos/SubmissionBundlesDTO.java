package org.canopyplatform.canopy.submissionservice.models.dtos;


import lombok.Data;
import java.util.List;

@Data
public class SubmissionBundlesDTO {
	private int submissionId;
	private List<BundleDTO> bundles;
	private List<BundleDTO> documents;
	private List<BundleDTO> unassigned;
}
