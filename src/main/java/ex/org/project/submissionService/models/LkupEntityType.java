package ex.org.project.submissionService.models;

import jakarta.persistence.*;
import lombok.Data;
@Entity
@Table(name = "lkup_entity_type")
@Data
public class LkupEntityType {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    private String name;

}

