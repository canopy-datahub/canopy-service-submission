package ex.org.project.submissionService.models.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class S3FileDTO {

    private String fileName;
    private String checksumHash;
    private Integer dataFileId;
    private Long fileSize;
    private Boolean uploadSuccessful;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String uploadErrorDescription;


}
