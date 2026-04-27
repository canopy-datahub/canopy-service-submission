package org.canopyplatform.canopy.submissionservice.models.dtos;

import lombok.Data;

@Data
public class DuplicateDownloadMapDTO {

    public DuplicateDownloadMapDTO(String key, DownloadDTO download){
        this.key = key;
        this.download = download;
    }

    private String key;
    private DownloadDTO download;
}
