package org.canopyplatform.canopy.submissionservice.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.time.Instant;


@Data
@Entity
@NoArgsConstructor
@Table(name = "s3_file", schema = "public")
public class S3File {

    public S3File(MultipartFile file, Integer submissionId, String studyUuid){
        this.fileName = file.getOriginalFilename();
        this.submissionId = submissionId;
        this.fileKey  = studyUuid + "/" + this.submissionId + "/" + this.fileName;
        this.fileType = file.getContentType();
        this.fileSize = file.getSize();
        this.toBeRemoved = false;
        this.uploadedAt = Timestamp.from(Instant.now());
        this.uploadedBy = 9999;
    }

    public S3File(MultipartFile file){
        this.fileName = file.getOriginalFilename();
        this.fileKey = file.getOriginalFilename();
        this.fileType = file.getContentType();
        this.fileSize = file.getSize();
        this.toBeRemoved = false;
        this.uploadedAt = Timestamp.from(Instant.now());
        this.uploadedBy = 9999;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String uuid;
    @Column(name = "s3_etag")
    private String s3Etag;
    @Transient
    private String serverSideEncryption;
    private String fileName;
    @Transient
    private Integer submissionId;
    @Transient
    private String fileKey;
    @Transient
    private String newFileKey;
    @Transient
    private String fileBucket;
    @Transient
    private String fileType;
    @Transient
    private String fileCategory;
    private Integer fileTypeId;
    private String filePath;
    @Transient
    private Long fileSize;
    private Boolean toBeRemoved;
    private String checksumHash;
    private String description;
    private Timestamp uploadedAt;
    private Integer uploadedBy;
    private Timestamp updatedAt;
    private Integer updatedBy;
    @Transient
    private Boolean uploadSuccessful;
    @Transient
    private String uploadErrorDescription;

    public void setS3FileKeyAndBucketFromPath() {
        if(this.filePath == null) {
            setFileKey(null);
            return;
        }
        //path form: bucket/path/to/file
        String[] parts = this.filePath.split("/", 2);
        setFileBucket(parts[0]);
        setFileKey(parts[1]);
    }

}
