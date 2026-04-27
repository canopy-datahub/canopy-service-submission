package org.canopyplatform.canopy.submissionservice.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "lkup_property_source")
@Data
public class LkupPropertySource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Integer id;

    private String name;

}
