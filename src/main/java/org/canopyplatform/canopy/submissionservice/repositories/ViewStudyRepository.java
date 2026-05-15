package org.canopyplatform.canopy.submissionservice.repositories;

import org.canopyplatform.canopy.submissionservice.models.ViewStudy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ViewStudyRepository extends JpaRepository<ViewStudy, Integer> {

    Optional<ViewStudy> findByStudyId(Integer studyId);

    List<ViewStudy> findByStudyIdIn(List<Integer> ids);

    List<ViewStudy> findByStudyIdInAndSubmissionStatusIn(List<Integer> studyIds, List<String> statuses);

    ViewStudy findByStudyIdOrderByCreatedAtDesc(Integer id);

    @Query(
            value = "select vs.* from view_study vs " +
                    "join data_submission ds on vs.study_id = ds.study_id " +
                    "where ds.id=:submissionId",
            nativeQuery = true
    )
    Optional<ViewStudy> findBySubmissionId(@Param("submissionId") Integer submissionId);

    @Query(
            value = "select vs.* from view_study vs join study s on vs.study_id=s.id where s.center_id=:centerId " +
                    "and vs.status in ('In Review', 'Draft') order by vs.status desc, vs.created_at",
            nativeQuery = true
    )
    List<ViewStudy> findAllInProgressByCenterId(Integer centerId);

    @Query(
            value = "select vs.* from view_study vs order by " +
                    "array_position(array['In Review', 'Draft', 'Approved'], vs.status), vs.created_at",
            nativeQuery = true
    )
    List<ViewStudy> findAllOrderedByStatus();

    @Query(
            value = "select vs.* from view_study vs join study s on vs.study_id=s.id " +
                    "where s.center_id=:centerId and vs.status='Approved' order by vs.study_id",
            nativeQuery = true
    )
    List<ViewStudy> findApprovedStudiesByCenterIdOrderByStudyId(Integer centerId);

    @Query(
            value = "select * from view_study where study_id in ( " +
                    "select distinct study_id from view_current_hub_content_data " +
                    "group by study_id " +
                    "having  count(submission_id) filter (where submission_id is null or submission_status != 'completed') = 0) " +
                    "and status='Approved' and center=:centerName " +
                    "order by study_id",
            nativeQuery = true
    )
    List<ViewStudy> findAllByCenterWithoutInProgressSubmissions(String centerName);

    @Query(nativeQuery = true,
    value = "select vs.study_id from view_study vs join study s on vs.study_id=s.id " +
            "join data_submission ds on ds.study_id = s.id where ds.id in :submissionIds")
    List<String> findStudyIdBySubmissionIds(Collection<Integer> submissionIds);

    @Query(
            value = "select vs.* from view_study vs join study s on vs.study_id=s.id where s.center_id=:centerId " +
                    "and vs.status =:status order by vs.created_at",
            nativeQuery = true
    )
    List<ViewStudy> findCenterStudiesByStatus(Integer centerId, String status);

    /**
     * Studies created by a specific user with a given status. Used by the
     * Data Submitter dashboard, which now scopes to "studies I created"
     * rather than "studies for my center."
     */
    @Query(
            value = "select vs.* from view_study vs join study s on vs.study_id=s.id where s.created_by=:creatorId " +
                    "and vs.status =:status order by vs.created_at",
            nativeQuery = true
    )
    List<ViewStudy> findCreatorStudiesByStatus(@Param("creatorId") Integer creatorId, @Param("status") String status);

    /**
     * Studies created by a specific user that have no in-progress submissions
     * — i.e. studies the user is eligible to start a new submission on.
     * Parallels findAllByCenterWithoutInProgressSubmissions but creator-scoped.
     */
    @Query(
            value = "select * from view_study where study_id in ( " +
                    "select distinct study_id from view_current_hub_content_data " +
                    "group by study_id " +
                    "having count(submission_id) filter (where submission_id is null or submission_status != 'completed') = 0) " +
                    "and status='Approved' and study_id in (select id from study where created_by=:creatorId) " +
                    "order by study_id",
            nativeQuery = true
    )
    List<ViewStudy> findAllByCreatorWithoutInProgressSubmissions(@Param("creatorId") Integer creatorId);


    List<ViewStudy> findAllBySubmissionStatusEqualsIgnoreCaseOrderByStudyId(String submissionStatus);

    @Query(
            value = "select vs.* from view_study vs where vs.status =:status order by vs.created_at",
            nativeQuery = true
    )
    List<ViewStudy> findCuratorStudiesByStatus(String status);
}
