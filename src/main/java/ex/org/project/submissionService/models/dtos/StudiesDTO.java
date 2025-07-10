package ex.org.project.submissionService.models.dtos;

import lombok.Data;

@Data
public class StudiesDTO {
    private Integer studyId;
    private String dcc; //Not actually dcc, should be phs + study title, frontend just expects the dcc key
    public StudiesDTO(Integer studyId, String dcc) {
        this.studyId = studyId;
        this.dcc = dcc;
    }
}
