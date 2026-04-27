package org.canopyplatform.canopy.submissionservice.repositories;

import org.canopyplatform.canopy.submissionservice.models.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyRepository extends JpaRepository<Study,Integer> {

    List<Study>findByCenter_Name(String centerName);

    List<Study>findByCenter_Id(Integer centerId);

    Study findStudyById(Integer id);

    Optional<Study> findStudyByIdAndStatusNameIsNot(Integer id,String statusName);

    @Query(nativeQuery = true,
            value="select s.* from study s join data_submission ds on s.id=ds.study_id where ds.id=?1"
    )
    Study findByDataSubmission_Id(Integer submissionId);

    Optional<Study> findById(Integer studyId);

    Study findStudyByUuidEqualsIgnoreCase(String uuid);

}

