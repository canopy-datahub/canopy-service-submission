package org.canopyplatform.canopy.submissionservice.models.dtos;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
public class UploadFilesDTO {

	private List<S3FileDTO> upload;
	private StudiesDTO studies;

	public UploadFilesDTO(List<S3FileDTO> upload, StudiesDTO studies) {
		this.upload = upload;
		this.studies = studies;
	}

}
