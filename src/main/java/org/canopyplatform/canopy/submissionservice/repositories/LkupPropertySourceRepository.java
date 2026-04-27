package org.canopyplatform.canopy.submissionservice.repositories;

import org.canopyplatform.canopy.submissionservice.models.LkupPropertySource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LkupPropertySourceRepository extends JpaRepository<LkupPropertySource, Integer> {

    Optional<LkupPropertySource> findByName(String name);

    List<LkupPropertySource> findAllByNameIn(List<String> sourceNames);

}
