package ex.org.project.submissionService.security;

import ex.org.project.datahub.auth.config.KeycloakSecurityConfig;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Security configuration for Submission Service.
 * Extends the shared Keycloak authentication library configuration.
 */
@Configuration
public class SecurityConfig extends KeycloakSecurityConfig {

  @Override
  protected void configureEndpointSecurity(
      AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth
  ) throws Exception {
    auth
        // Actuator endpoints
        .requestMatchers(EndpointRequest.to("health")).permitAll()
        .requestMatchers(EndpointRequest.to("shutdown")).authenticated()
        
        // All other endpoints require authentication
        .anyRequest().authenticated();
  }
}

