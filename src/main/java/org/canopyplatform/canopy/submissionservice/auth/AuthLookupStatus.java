package org.canopyplatform.canopy.submissionservice.auth;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lkup_status")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
class AuthLookupStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name")
    private String name;

}
