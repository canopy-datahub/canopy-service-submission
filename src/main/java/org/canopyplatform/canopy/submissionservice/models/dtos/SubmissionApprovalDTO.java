package org.canopyplatform.canopy.submissionservice.models.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmissionApprovalDTO {

	private String fileRejectionReason;
	private DetailsDTO submissionDetails;

}
