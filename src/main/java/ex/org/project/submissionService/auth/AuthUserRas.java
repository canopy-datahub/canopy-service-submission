package ex.org.project.submissionService.auth;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "user_ras")
public class AuthUserRas {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(name="phs")
    private String phs;

    @Column(name = "user_id")
    private Integer userId;

}
