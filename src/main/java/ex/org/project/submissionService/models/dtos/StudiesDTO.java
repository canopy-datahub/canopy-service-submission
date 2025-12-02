package ex.org.project.submissionService.models.dtos;

import lombok.Data;

@Data
public class StudiesDTO {
    private Integer studyId;
    private String center; //Not actually center, should be study_id + study title, frontend just expects the center key
    public StudiesDTO(Integer studyId, String center) {
        this.studyId = studyId;
        this.center = center;
    }
}
