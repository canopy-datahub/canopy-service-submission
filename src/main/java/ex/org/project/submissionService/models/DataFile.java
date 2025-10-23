package ex.org.project.submissionService.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "data_file", schema = "public")
public class DataFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "submission_id")
    private Integer submissionId;

    @Column(name = "source_file_name")
    private String sourceFileName;

    @Column(name = "normalized_file_name")
    private String normalizedFileName;

    @Column(name = "version_no")
    private Integer versionNumber;

    @Column(name = "is_current_version")
    private Boolean isCurrentVersion;

    @Column(name = "pii_phi")
    private Boolean piiPhiFailed;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_data_file_id")
    private DataFile originalDataFile;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_headers")
    private String fileHeaders;

    @Column(name = "validation_result")
    @JdbcTypeCode(SqlTypes.JSON)
    private String validationResults;

    @Column(name = "pii_phi_validation_result")
    @JdbcTypeCode(SqlTypes.JSON)
    private String piiPhivalidationResults;

    @Column(name = "cde_validation")
    private Boolean cdeValidationFailed;

    @Column(name = "acknowledged")
    private Boolean validationAcknowledged;

    @ManyToOne
    @JoinColumn(name="status_id",referencedColumnName = "id")
    private LkupStatus status;

    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name="s3_file_id", referencedColumnName = "id")
    private S3File s3File;

    @ManyToOne
    @JoinColumn(name="file_category_id")
    private DataFileCategory fileCategory;

    @Column(name="dictionary_file_id")
    private Integer dictionaryFileId;

    @Column(name="metadata_file_id")
    private Integer metadataFileId;

    @Column(name="dict_validation")
    private Boolean dictValidationFailed;

    @Column(name="meta_validation")
    private Boolean metaValidationFailed;

    @Column(name="variable_count")
    private Integer variablesCount;

    @Column(name="sample_size")
    private Integer sampleSize;

    @Column(name="created_by")
    private Integer createdBy;

    @Column(name="modified_by")
    private Integer modifiedBy;

    @Transient
    private Boolean willBeVersioned;

    public DataFile(S3File s3File, Integer submissionId, DataFileCategory fileCategory, String normalizedFileName, LkupStatus status) {
        this.sourceFileName = s3File.getFileName();
        this.submissionId = submissionId;
        this.isCurrentVersion = false;
        this.s3File = s3File;
        this.fileSize = s3File.getFileSize();
        this.normalizedFileName = normalizedFileName;
        this.fileCategory = fileCategory;
        this.status = status;
        this.validationAcknowledged = false;
    }

    /**
     * Updates this DataFile with a new S3File object reference
     * @param s3File The new S3File to be associated with this DataFile
     * @param fileCategory
     * @param normalizedFileName The file name after a normalization function has been applied
     * @param status The approval status for this DataFile to be set to, likely reset to draft
     */
    public void updateS3File(S3File s3File, DataFileCategory fileCategory, String normalizedFileName, LkupStatus status){
        this.sourceFileName = s3File.getFileName();
        this.s3File = s3File;
        this.fileSize = s3File.getFileSize();
        this.normalizedFileName = normalizedFileName;
        this.fileCategory = fileCategory;
        this.status = status;
        this.validationResults = null;
        this.validationAcknowledged = false;
    }
}
