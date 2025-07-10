package ex.org.project.submissionService.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "user_ras")
public class UserRas {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(name="phs")
    private String phs;


}