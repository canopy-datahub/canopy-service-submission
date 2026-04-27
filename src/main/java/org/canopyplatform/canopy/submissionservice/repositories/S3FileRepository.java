package org.canopyplatform.canopy.submissionservice.repositories;

import org.canopyplatform.canopy.submissionservice.models.S3File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface S3FileRepository extends JpaRepository<S3File, Integer> {

    S3File findS3FileByFilePath(String filePath);
}
