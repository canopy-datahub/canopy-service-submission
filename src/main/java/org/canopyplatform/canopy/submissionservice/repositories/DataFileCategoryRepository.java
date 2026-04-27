package org.canopyplatform.canopy.submissionservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.canopyplatform.canopy.submissionservice.models.DataFileCategory;

import java.util.List;

@Repository
public interface DataFileCategoryRepository extends JpaRepository<DataFileCategory, Integer> {
    DataFileCategory findByName(String name);
    List<DataFileCategory> findDataFileCategoriesByNameContainingIgnoreCase(String categoryName);
}

