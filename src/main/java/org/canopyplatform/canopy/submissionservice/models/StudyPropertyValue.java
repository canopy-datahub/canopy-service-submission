package org.canopyplatform.canopy.submissionservice.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@NoArgsConstructor
@Entity
@Table(name = "study_property_value")
@Data
public class StudyPropertyValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "study_id")
    private Integer studyId;

    @Column(name = "property_value")
    private String propertyValue;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "entity_property_id")
    private EntityProperty entityProperty;

    private Integer valueIndex;

    private Integer createdBy;

    private Timestamp modifiedAt;

    private Integer modifiedBy;

    @Transient
    private Boolean shouldBeRemoved;
}
