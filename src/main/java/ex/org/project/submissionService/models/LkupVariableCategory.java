package ex.org.project.submissionService.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "lkup_variable_category", schema = "public")
public class LkupVariableCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false)
    private String name;
    
    private String description;

    public LkupVariableCategory(Integer id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }
}

