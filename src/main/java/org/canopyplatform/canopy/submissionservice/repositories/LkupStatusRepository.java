package org.canopyplatform.canopy.submissionservice.repositories;
import org.canopyplatform.canopy.submissionservice.models.LkupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LkupStatusRepository extends JpaRepository<LkupStatus, Integer> {
    Optional<LkupStatus> findByUsageAndName(String usage, String name);

    Optional <LkupStatus> findByName(String name);
}
