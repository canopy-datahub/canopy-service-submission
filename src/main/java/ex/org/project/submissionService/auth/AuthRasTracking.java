package ex.org.project.submissionService.auth;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "ras_tracking")
class AuthRasTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "authorization_code")
    private String authorizationCode;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "access_token")
    private String accessToken;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "id_token")
    private String idToken;

    @Column(name = "passport")
    private String passport;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "modified_at")
    private ZonedDateTime updateAt;

    @PreUpdate
    public void setUpdatedAt() {
        this.updateAt = ZonedDateTime.now();
    }

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = ZonedDateTime.now();
    }

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "institution_name")
    private String institutionName;

}