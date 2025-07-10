package ex.org.project.submissionService.repositories;

import ex.org.project.submissionService.models.UserFileUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserFileUploadRepository extends JpaRepository<UserFileUpload, Integer> {

    @Query(nativeQuery = true,
            value="select * from user_file_upload ufu order by ufu.s3_file_id nulls last, ufu.download_at desc, ufu.upload_at asc"
    )
    List<UserFileUpload> findAllOrdered();

    List<UserFileUpload> findAllByStudyId(Integer studyId);
}
