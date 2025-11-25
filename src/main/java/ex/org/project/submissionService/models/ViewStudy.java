package ex.org.project.submissionService.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(name="phs")
    private String phs;

    @Column(name="title")
    private String studyName;

    @Column(name="status")
    private String submissionStatus;

    @Column(name="created_at")
    private Timestamp createdAt;

    @Column(name="has_data_files")
    private Boolean  hasDataFiles;

    private String center;

    public String getPhsTitle(){
        return String.format("(%s) %s", phs, studyName);
    }
}
