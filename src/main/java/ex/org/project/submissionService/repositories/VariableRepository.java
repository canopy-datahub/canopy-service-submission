package ex.org.project.submissionService.repositories;

import ex.org.project.submissionService.models.Variable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VariableRepository extends JpaRepository<Variable, Integer> {

    List<Variable> findByFileId(Integer fileId);

    void deleteByFileId(Integer fileId);
}

