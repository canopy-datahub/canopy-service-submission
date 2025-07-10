package ex.org.project.submissionService.repositories;

import ex.org.project.submissionService.models.S3FileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface S3FileTypeRepository extends JpaRepository<S3FileType, Integer> {

    S3FileType findByName(String name);

}
