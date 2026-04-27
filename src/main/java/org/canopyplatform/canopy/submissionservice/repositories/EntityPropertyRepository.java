package org.canopyplatform.canopy.submissionservice.repositories;

import org.canopyplatform.canopy.submissionservice.models.EntityProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntityPropertyRepository extends JpaRepository<EntityProperty, Integer> {

    List<EntityProperty> findAllByNameIn(List<String> names);

    Optional<EntityProperty> findByName(String name);

    List<EntityProperty> findAllByPropertySourceId(Integer propertySourceId);

    List<EntityProperty> findAllByPropertySourceIdIn(List<Integer> sourceIds);

}
