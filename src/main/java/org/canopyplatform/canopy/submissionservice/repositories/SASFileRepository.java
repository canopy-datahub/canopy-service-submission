package org.canopyplatform.canopy.submissionservice.repositories;

import org.canopyplatform.canopy.submissionservice.models.SASFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SASFileRepository extends JpaRepository<SASFile, Integer> {

    List<SASFile> findSASFilesByDataFileId(Integer dataFileId);
}
