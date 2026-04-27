package org.canopyplatform.canopy.submissionservice.repositories;

import org.canopyplatform.canopy.submissionservice.models.LkupCoreVariablePropertyValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LkupCoreVariablePropertyValueRepository extends JpaRepository<LkupCoreVariablePropertyValue, Integer> {

    List<LkupCoreVariablePropertyValue> findByVariableName(String variableName);

    List<LkupCoreVariablePropertyValue> findByVariableNameAndEntityPropertyId(String variableName, Integer entityPropertyId);
}

