package ex.org.project.submissionService.repositories;

import ex.org.project.submissionService.models.DataFileVariable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataFileVariableRepository  extends JpaRepository<DataFileVariable, Integer> {
    List<DataFileVariable> findByDataFileId(Integer dataFileId);
}
