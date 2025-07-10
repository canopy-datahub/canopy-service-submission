package ex.org.project.submissionService.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "entity_property_mta_mapping")
@Data
public class EntityPropertyMtaMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Integer id;

    private Integer entityPropertyId;

    private String pdfFieldName;

    private Integer codelistId;

    private Integer codelistValueId;

    private String description;
}
