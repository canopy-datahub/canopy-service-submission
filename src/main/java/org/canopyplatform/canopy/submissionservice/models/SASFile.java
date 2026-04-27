package org.canopyplatform.canopy.submissionservice.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Data
@NoArgsConstructor
@Table(name = "sas_data_file")
public class SASFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "parent_data_file_id")
    private Integer dataFileId;

    @Column(name = "source_file_name")
    private String fileName;

    @ManyToOne
    @JoinColumn(name="file_category_id")
    private DataFileCategory fileCategory;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "status_id")
    private Integer statusId;

    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name="s3_file_id", referencedColumnName = "id")
    private S3File s3File;

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "modified_by")
    private Integer modifiedBy;

    @Column(name = "modified_at")
    private Timestamp modifiedAt;
}
