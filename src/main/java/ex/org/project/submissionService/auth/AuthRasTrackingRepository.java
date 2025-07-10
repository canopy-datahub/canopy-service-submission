package ex.org.project.submissionService.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
interface AuthRasTrackingRepository extends JpaRepository<AuthRasTracking, Integer> {

    Optional<AuthRasTracking> findBySessionId(String sessionId);

}
