package ex.org.project.submissionService.repositories;

import ex.org.project.submissionService.models.LkupPropertyCodelistValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LkupPropertyCodelistValueRepository extends JpaRepository<LkupPropertyCodelistValue, Integer> {

    List<LkupPropertyCodelistValue> findAllByPropertyCodelistId(Integer codelistId);

}
