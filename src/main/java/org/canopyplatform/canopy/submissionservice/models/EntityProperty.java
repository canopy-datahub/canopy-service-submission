package org.canopyplatform.canopy.submissionservice.models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "entity_property")
@Data
public class EntityProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "entity_type_id")
    private LkupEntityType lkupEntityType;

    @Column(name = "name")
    private String name;

    private Integer codeListId;

    @JoinColumn(name = "property_source_id")
    private Integer propertySourceId;

}
