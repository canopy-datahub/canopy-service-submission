package ex.org.project.submissionService.models;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Entity
@Table(name = "study")
@Data
public class Study {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dcc_id")
    private LkupDCC dcc;

    private String fileName;

    @Column(name="file_url")
    private String fileUrl;

    private Timestamp createdAt;

    private Integer createdBy;

    @Transient
    private String bucketName;

    @Transient
    private String ObjectKey;

    private Timestamp modifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    private LkupStatus status;

    public void extractBucketAndObjectKeyFromS3Uri(String s3Uri) {
        // Implement the logic to extract the bucket name and object key from the S3 URI
        String[] uriParts = s3Uri.split("/",2);
        setBucketName(uriParts[0]);
        setObjectKey(uriParts[1]);
    }

}