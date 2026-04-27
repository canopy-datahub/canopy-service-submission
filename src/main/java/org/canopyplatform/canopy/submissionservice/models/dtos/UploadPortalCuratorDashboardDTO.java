package org.canopyplatform.canopy.submissionservice.models.dtos;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class UploadPortalCuratorDashboardDTO {

    private Integer id;
    private String study;
    private String uploadBy;
    private Timestamp uploadAt;
    private Timestamp downloadAt;
    private Boolean isDeleted;
    private Timestamp deleteAt;

}
