package org.canopyplatform.canopy.submissionservice.repositories;

import org.canopyplatform.canopy.submissionservice.models.SASFileDownload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SASFileDownloadRepository extends JpaRepository<SASFileDownload, Integer> {

    List<SASFileDownload> findSASFileDownloadBySasFileId(Integer sasFileId);
}
