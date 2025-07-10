package ex.org.project.submissionService.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "user_file_upload", schema = "public")
public class UserFileUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "study_id")
    private Integer studyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="study_id", referencedColumnName = "study_id", insertable=false, updatable=false)
    private ViewStudy viewStudy;

    @Column(name = "file_name")
    private String fileName;

    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name="s3_file_id", referencedColumnName = "id")
    private S3File s3File;

    @Column(name = "upload_by")
    private Integer uploadBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="upload_by", insertable=false, updatable=false)
    private Users uploadUser;

    @Column(name = "upload_at")
    private Timestamp uploadAt;

    @Column(name = "download_by")
    private Integer downloadBy;

    @Column(name = "download_at")
    private Timestamp downloadAt;

    @Column(name = "delete_by")
    private Integer deleteBy;

    @Column(name = "delete_at")
    private Timestamp deleteAt;

    public UserFileUpload(S3File s3File, Integer studyId) {
        this.s3File = s3File;
        this.studyId = studyId;
        this.uploadBy = s3File.getUploadedBy();
        this.fileName = s3File.getFileName();
        this.uploadAt = s3File.getUploadedAt();
    }

    public void removeS3FileLink(Integer userId){
        this.s3File = null;
        this.setDeleteBy(userId);
        this.setDeleteAt(Timestamp.from(Instant.now()));
    }

}
