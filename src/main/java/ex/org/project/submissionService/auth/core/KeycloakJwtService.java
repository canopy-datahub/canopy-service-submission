package ex.org.project.submissionService.auth.core;

import ex.org.project.submissionService.auth.UserAuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * Service for extracting user information from Keycloak JWT tokens
 */
@Service
@Slf4j
public class KeycloakJwtService {

    /**
     * Extract email from JWT token
     * Tries 'email' claim first, falls back to 'preferred_username'
     *
     * @param jwt JWT token from SecurityContext
     * @return User's email address
     * @throws UserAuthenticationException if email claim is missing
     */
    public String extractEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isEmpty()) {
            email = jwt.getClaimAsString("preferred_username");
        }
        if (email == null || email.isEmpty()) {
            log.error("JWT token does not contain email or preferred_username claim. Available claims: {}",
                jwt.getClaims().keySet());
            throw new UserAuthenticationException(
                "JWT token does not contain email or preferred_username claim"
            );
        }
        return email;
    }

    /**
     * Extract subject (unique user ID) from JWT token
     *
     * @param jwt JWT token from SecurityContext
     * @return User's subject (sub claim)
     */
    public String extractSubject(Jwt jwt) {
        return jwt.getSubject();
    }

    /**
     * Extract user's name from JWT token
     *
     * @param jwt JWT token from SecurityContext
     * @return User's name or preferred_username as fallback
     */
    public String extractName(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        if (name == null) {
            return jwt.getClaimAsString("preferred_username");
        }
        return name;
    }
}

