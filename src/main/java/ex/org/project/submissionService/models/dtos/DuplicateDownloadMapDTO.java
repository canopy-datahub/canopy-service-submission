package ex.org.project.submissionService.models.dtos;

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
