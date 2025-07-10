
package ex.org.project.submissionService.repositories;

import ex.org.project.submissionService.models.StudyPropertyValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyPropertyValueRepository extends JpaRepository<StudyPropertyValue, Integer> {

    List<StudyPropertyValue> findByEntityProperty_NameAndEntityProperty_LkupEntityType_NameAndStudyIdIn(String entityPropertyname, String lkupEntityname, List<Integer> studyIds);

    StudyPropertyValue findByEntityProperty_NameAndEntityProperty_LkupEntityType_NameAndStudyId(String entityPropertyname, String lkupEntityname, Integer studyId);

    List<StudyPropertyValue> findAllByStudyId(Integer studyId);

    StudyPropertyValue findByEntityProperty_NameAndStudyId(String entityPropertyName,Integer studyId);

    void deleteAllByStudyId(Integer studyId);
}
