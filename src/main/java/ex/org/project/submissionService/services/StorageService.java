package ex.org.project.submissionService.services;

import ex.org.project.submissionService.models.S3File;
import ex.org.project.submissionService.models.Study;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StorageService {

    S3File uploadFile(MultipartFile file, Integer submissionId, Integer userId);

    S3File uploadFile(MultipartFile file, Integer submissionId, String studyUuid, Integer userId);

    boolean moveToApproved(List<S3File> s3Files);

    boolean deleteStudyFromS3(Study study);

}

