package ex.org.project.submissionService.repositories;

import ex.org.project.submissionService.models.EntityPropertyMtaMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntityPropertyMtaMappingRepository extends JpaRepository<EntityPropertyMtaMapping, Integer> {
}
