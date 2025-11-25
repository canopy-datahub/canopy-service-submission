package ex.org.project.submissionService.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collections;

/**
 * Security configuration for Submission Service.
 * 
 * Provides two security filter chains:
 * 1. HTTP Basic Auth for actuator endpoints (shutdown, health)
 * 2. Keycloak JWT for all other API endpoints
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  /**
   * Security filter chain for actuator endpoints using HTTP Basic Auth.
   * This allows management operations (like shutdown) to use simple username/password.
   * 
   * @param http HttpSecurity configuration
   * @return Configured SecurityFilterChain for actuator endpoints
   * @throws Exception if configuration fails
   */
  @Bean
  @Order(1) // Higher priority than the API filter chain
  public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher("/api/submission-service/v1/actuator/**")  // Match actuator path explicitly
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/submission-service/v1/actuator/health").permitAll()
            .requestMatchers("/api/submission-service/v1/actuator/shutdown").authenticated()
            .anyRequest().authenticated()
        )
        .csrf(csrf -> csrf
            .ignoringRequestMatchers("/api/submission-service/v1/actuator/shutdown")
        )
        .httpBasic(httpBasic -> {}); // Enable HTTP Basic Auth for actuator endpoints

    return http.build();
  }

  /**
   * Security filter chain for API endpoints using Keycloak JWT.
   * This is the main application security configuration.
   * 
   * @param http HttpSecurity configuration
   * @return Configured SecurityFilterChain for API endpoints
   * @throws Exception if configuration fails
   */
  @Bean
  @Order(2) // Lower priority - handles all non-actuator requests
  public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        // Explicitly match only non-actuator paths
        .securityMatcher(request -> !request.getRequestURI().startsWith("/api/submission-service/v1/actuator"))
        
        // Disable CSRF - not needed for stateless JWT authentication
        .csrf(csrf -> csrf.disable())
        
        // Authorize requests
        .authorizeHttpRequests(auth -> auth
            // Submission endpoints
            .requestMatchers(
                "/getStudies",
                "/create-submission",
                "/getCategories",
                "/submissionInfo",
                "/deleteFiles",
                "/replaceFile"
            ).authenticated()
            // Curator endpoints
            .requestMatchers(
                "/curator/getSubmissions",
                "/curator/getFilesBySubm",
                "/curator/processFiles",
                "/curator/all-submission-files"
            ).authenticated()
            // Upload portal endpoints
            .requestMatchers(
                "/uploadPortal/upload/**",
                "/uploadPortal/getStudies",
                "/uploadPortal/curator/dashboard",
                "/uploadPortal/curator/dashboard/delete"
            ).authenticated()
            // Study registration endpoints
            .requestMatchers(
                "/study/curator/create",
                "/study/center/create",
                "/study/getValues",
                "/study/curator/edit",
                "/study/center/edit",
                "/study/center/studies",
                "/study/curator/studies",
                "/study/delete"
            ).authenticated()
            // Upload files endpoints
            .requestMatchers(
                "/uploadFiles/getFiles",
                "/uploadFiles/multiple",
                "/uploadFiles/createBundles",
                "/uploadFiles/processSFTP"
            ).authenticated()
            // Validation endpoints
            .requestMatchers(
                "/validateFiles/validate",
                "/validateFiles/getResults",
                "/validateFiles/acknowledge"
            ).authenticated()
            // Submitter dashboard endpoints
            .requestMatchers(
                "/getSubmissions",
                "/deleteSubmission"
            ).authenticated()
            // Review and submit endpoints
            .requestMatchers(
                "/reviewAndSubmit/submit"
            ).authenticated()
            // Download endpoints
            .requestMatchers(
                "/download/validationErrorsByFile",
                "/download/validationErrorsBySubmission"
            ).authenticated()
            // Bundle endpoints
            .requestMatchers(
                "/bundle/get",
                "/bundle/update",
                "/bundle/delete",
                "/bundle/getFiles",
                "/bundle/previousPage"
            ).authenticated()
            // All other endpoints require authentication
            .anyRequest().authenticated()
        )
        
        // Enable OAuth2 Resource Server with JWT
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
        )
        
        // Stateless session - no session storage
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

    return http.build();
  }

  /**
   * JWT authentication converter.
   * Configured to NOT extract roles from JWT - roles come from database.
   * 
   * @return Configured JwtAuthenticationConverter
   */
  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    // Don't extract roles from JWT - we use database roles instead
    converter.setJwtGrantedAuthoritiesConverter(jwt -> Collections.emptyList());
    return converter;
  }
}
