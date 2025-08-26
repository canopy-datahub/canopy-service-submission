package ex.org.project.submissionService.repositories;

import ex.org.project.submissionService.models.LkupDCC;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LkupDCCRepository extends JpaRepository<LkupDCC, Integer> {

    Optional<LkupDCC> findByNameContainingIgnoreCase(String partialName);
    LkupDCC findByName( String alternateName);

    Optional<LkupDCC> findByNameEqualsIgnoreCase(String dccName);
}
