package ex.org.project.submissionService.repositories;

import ex.org.project.submissionService.models.DataSubmission;
import ex.org.project.submissionService.models.LkupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataSubmissionRepository extends JpaRepository<DataSubmission, Integer> {

	DataSubmission findDataSubmissionById(Integer submissionId);

	List<DataSubmission> findDistinctByStudyId(Integer studyId);
	List<DataSubmission> findDataSubmissionsByStatusOrderByDateSubmittedDesc(LkupStatus status);

	List<DataSubmission> findBySubmitterUserIdOrderByCreatedAtDesc(Integer id);

	@Query(nativeQuery = true,
			value="select ds.* from data_submission ds where ds.study_id = :study_id and submitter_user_id = :user_id and status_id = :status and is_validated is false and (step_id = :step_id or step_id is null)"
	)
	List<DataSubmission> findInProgressDataSubmission(@Param("study_id") Integer studyId,
													  @Param("user_id") Integer userId,
													  @Param("status")Integer status,
													  @Param("step_id") Integer stepId);

	List<DataSubmission> findBySubmitterUserIdOrderByDateSubmittedDesc(Integer id);

	List<DataSubmission> findBySubmitterUserIdAndStatusOrderByModifiedAtDesc(Integer id, LkupStatus status);

}
