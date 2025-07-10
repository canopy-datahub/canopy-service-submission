package ex.org.project.submissionService.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SftpUploadInfo {
    private Users user;
    private List<String> studies;
}
