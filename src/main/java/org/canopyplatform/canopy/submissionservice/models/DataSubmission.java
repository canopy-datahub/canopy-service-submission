package org.canopyplatform.canopy.submissionservice.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Data
@Table(name = "data_submission")
@NoArgsConstructor
@SequenceGenerator(name = "data_submission_id_seq", sequenceName = "data_submission_id_seq", allocationSize = 1)
public class DataSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "data_submission_id_seq")
    @Column(name = "id")
    private Integer id;

    @Column(name="study_id")
    private Integer studyId;

    @Column(name="status_id")
    private Integer statusId;

    @Column(name="submitter_user_id")
    private Integer submitterUserId;

    @ManyToOne
    @JoinColumn(name = "step_id")
    private LkupSubmissionStep stepId;

    @Column(name="is_validated")
    private boolean isValidated;

    @Column(name="date_submitted")
    private Timestamp dateSubmitted;

    @ManyToOne
    @JoinColumn(name="study_id", insertable = false, updatable = false)
    private ViewStudy study;

    @Column(name="created_at")
    private Timestamp createdAt;

    @Column(name="file_rejection_reason")
    private String fileRejectionReason;

    @Column(name="file_rejected_count")
    private Integer fileRejectedCount;

    @Column(name = "modified_at")
    private Timestamp modifiedAt;

    @Column(name = "modified_by")
    private Integer modifiedBy;

    @Column(name = "date_approved")
    private Timestamp dateApproved;

    @ManyToOne
    @JoinColumn(name="status_id", insertable = false, updatable = false)
    private LkupStatus status;

    public DataSubmission(Integer studyId, Integer submitterUserId, LkupStatus status) {
        this.studyId = studyId;
        this.submitterUserId = submitterUserId;
        this.status = status;
        this.statusId = status.getId();
    }
}
