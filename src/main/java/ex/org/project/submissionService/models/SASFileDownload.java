package ex.org.project.submissionService.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Data
@NoArgsConstructor
@Table(name = "sas_file_download")
public class SASFileDownload {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "sas_file_id")
    private Integer sasFileId;

    @Column(name = "download_by")
    private Integer downloadBy;

    @Column(name = "download_at")
    private Timestamp downloadAt;
}
