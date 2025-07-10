package ex.org.project.submissionService.repositories;

import ex.org.project.submissionService.models.S3File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface S3FileRepository extends JpaRepository<S3File, Integer> {

    S3File findS3FileByFilePath(String filePath);
}
