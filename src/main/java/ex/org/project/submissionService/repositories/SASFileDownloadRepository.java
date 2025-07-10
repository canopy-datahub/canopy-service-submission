package ex.org.project.submissionService.repositories;

import ex.org.project.submissionService.models.SASFileDownload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SASFileDownloadRepository extends JpaRepository<SASFileDownload, Integer> {

    List<SASFileDownload> findSASFileDownloadBySasFileId(Integer sasFileId);
}
