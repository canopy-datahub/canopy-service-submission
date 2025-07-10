package ex.org.project.submissionService.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "data_file_variable", schema = "public")
public class DataFileVariable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "data_file_id")
    private Integer dataFileId;

    @Column(name = "variable")
    private String variable;
}
