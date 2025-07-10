package ex.org.project.submissionService.repositories;

import ex.org.project.submissionService.models.DataFileDownload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataFileDownloadRepository extends JpaRepository<DataFileDownload, Integer> {

    List<DataFileDownload> findDataFileDownloadsByDataFileId(Integer dataFileId);
}
