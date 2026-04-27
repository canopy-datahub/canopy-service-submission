package org.canopyplatform.canopy.submissionservice.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Data
@Table(name="view_study")
@NoArgsConstructor
@AllArgsConstructor
public class ViewStudy {
    @Id
    @Column(name = "study_id")
    private Integer studyId;

    @Column(name="title")
    private String studyName;

    @Column(name="status")
    private String submissionStatus;

    @Column(name="created_at")
    private Timestamp createdAt;

    @Column(name="has_data_files")
    private Boolean  hasDataFiles;

    private String center;

    public String getStudyIdTitle(){
        return String.format("(%s) %s", studyId, studyName);
    }
}
