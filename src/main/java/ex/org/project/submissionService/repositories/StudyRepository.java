package ex.org.project.submissionService.repositories;

import ex.org.project.submissionService.models.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyRepository extends JpaRepository<Study,Integer> {

    List<Study>findByDcc_Name(String dccName);

    List<Study>findByDcc_Id(Integer dccId);

    Study findStudyById(Integer id);

    Optional<Study> findStudyByIdAndStatusNameIsNot(Integer id,String statusName);

    @Query(nativeQuery = true,
            value="select s.* from study s join data_submission ds on s.id=ds.study_id where ds.id=?1"
    )
    Study findByDataSubmission_Id(Integer submissionId);

    Optional<Study> findById(Integer studyId);

    Study findStudyByUuidEqualsIgnoreCase(String uuid);

}

