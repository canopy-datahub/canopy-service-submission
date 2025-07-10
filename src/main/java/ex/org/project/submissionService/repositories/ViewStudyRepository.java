package ex.org.project.submissionService.repositories;

import ex.org.project.submissionService.models.ViewStudy;
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
            value = "select vs.* from view_study vs join study s on vs.study_id=s.id where s.dcc_id=:dccId " +
                    "and vs.status in ('In Review', 'Pending DCC Input') order by vs.status desc, vs.created_at",
            nativeQuery = true
    )
    List<ViewStudy> findAllInProgressByDccId(Integer dccId);

    @Query(
            value = "select vs.* from view_study vs order by " +
                    "array_position(array['In Review', 'Pending DCC Input', 'Approved'], vs.status), vs.created_at",
            nativeQuery = true
    )
    List<ViewStudy> findAllOrderedByStatus();

    @Query(
            value = "select vs.* from view_study vs join study s on vs.study_id=s.id " +
                    "where s.dcc_id=:dccId and vs.status='Approved' order by vs.phs",
            nativeQuery = true
    )
    List<ViewStudy> findApprovedStudiesByDccIdOrderByPhs(Integer dccId);

    @Query(
            value = "select * from view_study where phs in ( " +
                    "select distinct study_phs from view_current_hub_content_data " +
                    "group by study_phs " +
                    "having  count(submission_id) filter (where submission_id is null or submission_status != 'completed') = 0) " +
                    "and status='Approved' and dcc=:dccName " +
                    "order by phs",
            nativeQuery = true
    )
    List<ViewStudy> findAllByDccWithoutInProgressSubmissions(String dccName);

    @Query(nativeQuery = true,
    value = "select vs.phs from view_study vs join study s on vs.study_id=s.id " +
            "join data_submission ds on ds.study_id = s.id where ds.id in :submissionIds")
    List<String> findPhsBySubmissionIds(Collection<Integer> submissionIds);

    @Query(
            value = "select vs.* from view_study vs join study s on vs.study_id=s.id where s.dcc_id=:dccId " +
                    "and vs.status =:status order by vs.created_at",
            nativeQuery = true
    )
    List<ViewStudy> findDCCStudiesByStatus(Integer dccId, String status);


    List<ViewStudy> findAllBySubmissionStatusEqualsIgnoreCaseOrderByPhs(String submissionStatus);

    @Query(
            value = "select vs.* from view_study vs where vs.status =:status order by vs.created_at",
            nativeQuery = true
    )
    List<ViewStudy> findCuratorStudiesByStatus(String status);
}
