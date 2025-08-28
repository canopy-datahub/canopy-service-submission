package ex.org.project.submissionService.repositories;

import ex.org.project.submissionService.models.LkupCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LkupCenterRepository extends JpaRepository<LkupCenter, Integer> {

    Optional<LkupCenter> findByNameContainingIgnoreCase(String partialName);
    LkupCenter findByName(String alternateName);

    Optional<LkupCenter> findByNameEqualsIgnoreCase(String dccName);
}
