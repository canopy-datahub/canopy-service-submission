package ex.org.project.submissionService.repositories;

import ex.org.project.submissionService.models.Institution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Integer> {

    Optional<Institution> findByAlternateName(String dccName);

}