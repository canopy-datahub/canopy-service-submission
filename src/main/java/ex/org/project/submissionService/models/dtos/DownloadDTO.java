package ex.org.project.submissionService.models.dtos;

import lombok.Data;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.FileDownload;

import java.io.File;

@Data
public class DownloadDTO {

    public DownloadDTO(String fileName, String s3Key, String s3Bucket){
        this.fileName = fileName;
        this.s3Key = s3Key;
        this.s3Bucket = s3Bucket;
    }

    private String fileName;
    private String s3Key;
    private String s3Bucket;
    private FileDownload fileDownload;
    private CompletedFileDownload completedFileDownload;
    private File localFile;

}
