package org.canopyplatform.canopy.submissionservice.repositories;

import org.canopyplatform.canopy.submissionservice.models.DataFileDownload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataFileDownloadRepository extends JpaRepository<DataFileDownload, Integer> {

    List<DataFileDownload> findDataFileDownloadsByDataFileId(Integer dataFileId);
}
