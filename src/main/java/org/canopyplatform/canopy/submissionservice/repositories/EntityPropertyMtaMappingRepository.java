package org.canopyplatform.canopy.submissionservice.repositories;

import org.canopyplatform.canopy.submissionservice.models.EntityPropertyMtaMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntityPropertyMtaMappingRepository extends JpaRepository<EntityPropertyMtaMapping, Integer> {
}
