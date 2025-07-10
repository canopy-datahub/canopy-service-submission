package ex.org.project.submissionService.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.Date;

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

    private String DCC;

    public String getPhsTitle(){
        return String.format("(%s) %s", phs, studyName);
    }
}
