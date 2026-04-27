package org.canopyplatform.canopy.submissionservice.repositories;

import org.canopyplatform.canopy.submissionservice.models.LkupVariableCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LkupVariableCategoryRepository extends JpaRepository<LkupVariableCategory, Integer> {

    Optional<LkupVariableCategory> findByName(String name);

    Optional<LkupVariableCategory> findByNameIgnoreCase(String name);
}

