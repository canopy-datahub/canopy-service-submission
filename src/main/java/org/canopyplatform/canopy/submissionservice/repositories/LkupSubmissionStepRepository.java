package org.canopyplatform.canopy.submissionservice.repositories;

import org.canopyplatform.canopy.submissionservice.models.LkupSubmissionStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LkupSubmissionStepRepository extends JpaRepository<LkupSubmissionStep, Integer> {

	Optional<LkupSubmissionStep> findById(Integer id);

	Optional<LkupSubmissionStep> findByDescription(String description);

}
