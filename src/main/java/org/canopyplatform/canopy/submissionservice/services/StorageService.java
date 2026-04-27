package org.canopyplatform.canopy.submissionservice.services;

import org.canopyplatform.canopy.submissionservice.models.S3File;
import org.canopyplatform.canopy.submissionservice.models.Study;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StorageService {

    S3File uploadFile(MultipartFile file, Integer submissionId, Integer userId);

    S3File uploadFile(MultipartFile file, Integer submissionId, String studyUuid, Integer userId);

    boolean moveToApproved(List<S3File> s3Files);

    boolean deleteStudyFromS3(Study study);

}

