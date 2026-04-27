package org.canopyplatform.canopy.submissionservice.models.dtos;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SubmissionStepDTO {

    private Integer id;
    private String description;
    private boolean validated;
    private String sftpKey;
}


