package ex.org.project.submissionService.repositories;

import ex.org.project.submissionService.models.DataFileCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataFileCategoryRepository extends JpaRepository<DataFileCategory, Integer> {
    DataFileCategory findByName(String name);
    List<DataFileCategory> findDataFileCategoriesByNameContainingIgnoreCase(String categoryName);
}

