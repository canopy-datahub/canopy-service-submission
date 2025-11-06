package ex.org.project.submissionService.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Data
@NoArgsConstructor
@Table(name = "lkup_core_variable_property_value")
public class LkupCoreVariablePropertyValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "variable_name", nullable = false)
    private String variableName;

    @Column(name = "entity_property_id", nullable = false)
    private Integer entityPropertyId;

    @Column(name = "property_value")
    private String propertyValue;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "modified_at")
    private Timestamp modifiedAt;

    @Column(name = "modified_by")
    private Integer modifiedBy;
}

