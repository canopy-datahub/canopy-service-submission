package org.canopyplatform.canopy.submissionservice.auth.core;

import org.canopyplatform.canopy.submissionservice.auth.UserAuthenticationException;
import org.canopyplatform.canopy.submissionservice.auth.UserAuthorizationException;
import org.canopyplatform.canopy.submissionservice.auth.AccessRole;
import org.canopyplatform.canopy.submissionservice.auth.AuthLookupStatusRepository;
import org.canopyplatform.canopy.submissionservice.auth.AuthRole;
import org.canopyplatform.canopy.submissionservice.auth.AuthUser;
import org.canopyplatform.canopy.submissionservice.auth.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Service for authenticating users from JWT tokens and checking authorization.
 * This is the main service that microservices will use for authentication.
 *
 * <p>Features:
 * <ul>
 *   <li>Extracts user information from Keycloak JWT tokens</li>
 *   <li>Loads user details from database</li>
 *   <li>Validates user roles for authorization</li>
 *   <li>Provides simple API for controllers</li>
 * </ul>
 *
 * <p>Example usage in controllers:
 * <pre>
 * {@code
 * @GetMapping("/protected")
 * public String endpoint(@AuthenticationPrincipal Jwt jwt) {
 *     Integer userId = authenticationService.checkAuth(jwt);
 *     // ... use userId
 * }
 *
 * @GetMapping("/admin")
 * public String adminEndpoint(@AuthenticationPrincipal Jwt jwt) {
 *     Integer userId = authenticationService.checkAuth(jwt, List.of(AccessRole.ADMIN));
 *     // ... admin only logic
 * }
 * }
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KeycloakAuthenticationService {

    private static final int STATUS_ACTIVE = 1;

    private final AuthUserRepository authUserRepository;
    private final AuthLookupStatusRepository authLookupStatusRepository;
    private final KeycloakJwtService keycloakJwtService;

    /**
     * Get authenticated user from JWT token, provisioning a new row on first login.
     *
     * <p>The JWT signature has already been verified by Spring Security against
     * Keycloak's public key (JWK Set URI), so the {@code sub} claim is guaranteed
     * to be the genuine Keycloak UUID — it cannot be forged.
     *
     * @param jwt JWT token from SecurityContext
     * @return AuthUser entity with roles eagerly loaded
     * @throws UserAuthenticationException if JWT is missing required claims
     */
    @Transactional
    public AuthUser getAuthenticatedUser(Jwt jwt) {
        if (jwt == null) {
            throw new UserAuthenticationException("No authentication token provided");
        }
        String keycloakUuid = keycloakJwtService.extractSubject(jwt);
        log.debug("Authenticating user with Keycloak UUID: {}", keycloakUuid);

        return authUserRepository.findByUuid(keycloakUuid)
                .orElseGet(() -> provisionUser(jwt, keycloakUuid));
    }

    /**
     * Create a new user row from Keycloak JWT claims on first login (JIT provisioning).
     */
    private AuthUser provisionUser(Jwt jwt, String keycloakUuid) {
        String email = keycloakJwtService.extractEmail(jwt);
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");

        log.info("Provisioning new user from Keycloak: uuid={}, email={}", keycloakUuid, email);

        AuthUser newUser = new AuthUser();
        newUser.setUuid(keycloakUuid);
        newUser.setEmail(email);
        newUser.setFirstName(firstName != null ? firstName : "");
        newUser.setLastName(lastName != null ? lastName : "");
        newUser.setInternalUser(false);
        newUser.setStatus(authLookupStatusRepository.getReferenceById(STATUS_ACTIVE));
        newUser.setRoles(Collections.emptyList());

        return authUserRepository.save(newUser);
    }

    /**
     * Get authenticated user ID from JWT token.
     * Convenience method that returns just the user ID.
     *
     * @param jwt JWT token from SecurityContext
     * @return User ID
     * @throws UserAuthenticationException if JWT is invalid
     * @throws UserNotFoundException if user not found in database
     */
    public Integer getAuthenticatedUserId(Jwt jwt) {
        return getAuthenticatedUser(jwt).getId();
    }

    /**
     * Check if user has any of the required roles.
     *
     * @param user AuthUser entity with roles loaded
     * @param requiredRoles List of required access roles (empty = no role requirement)
     * @return true if user has any of the required roles, or if no roles required
     */
    public boolean hasAnyRole(AuthUser user, List<AccessRole> requiredRoles) {
        if (requiredRoles == null || requiredRoles.isEmpty()) {
            return true;  // No specific role required
        }

        List<String> userRoleNames = user.getRoles().stream()
                .map(AuthRole::getName)
                .toList();

        for (AccessRole requiredRole : requiredRoles) {
            if (userRoleNames.contains(requiredRole.label)) {
                log.debug("User {} has required role: {}", user.getEmail(), requiredRole.label);
                return true;
            }
        }

        return false;
    }

    /**
     * Check authentication and authorization.
     * Validates JWT, loads user, and checks if user has any of the required roles.
     *
     * @param jwt JWT token from SecurityContext
     * @param requiredRoles Required roles (empty list = any authenticated user)
     * @return User ID if authorized
     * @throws UserAuthenticationException if user not found
     * @throws UserAuthorizationException if user doesn't have required role
     */
    public Integer checkAuth(Jwt jwt, List<AccessRole> requiredRoles) {
        AuthUser user = getAuthenticatedUser(jwt);

        if (!hasAnyRole(user, requiredRoles)) {
            log.warn("User {} attempted access without required roles. Required: {}, User has: {}",
                    user.getEmail(), requiredRoles,
                    user.getRoles().stream().map(AuthRole::getName).toList());
            throw new UserAuthorizationException(
                "User does not have the necessary role for access"
            );
        }

        return user.getId();
    }

    /**
     * Check authentication only (no role requirement).
     * Validates JWT and loads user without checking roles.
     *
     * @param jwt JWT token from SecurityContext
     * @return User ID
     * @throws UserAuthenticationException if user not found
     */
    public Integer checkAuth(Jwt jwt) {
        return checkAuth(jwt, Collections.emptyList());
    }
}

