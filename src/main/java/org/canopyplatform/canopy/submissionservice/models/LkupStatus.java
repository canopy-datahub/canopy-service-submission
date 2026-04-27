package org.canopyplatform.canopy.submissionservice.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
@Entity
@Data
@RequiredArgsConstructor
@Table(name = "lkup_status")
public class LkupStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Integer id;

    private String name;

    private String usage;

}
