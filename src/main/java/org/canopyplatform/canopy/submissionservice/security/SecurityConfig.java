package org.canopyplatform.canopy.submissionservice.security;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collections;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(EndpointRequest.to("shutdown")).authenticated()
            .anyRequest().permitAll()
        )
        // .csrf(csrf -> csrf
        //     .ignoringRequestMatchers(EndpointRequest.to("shutdown"))
        //     .ignoringRequestMatchers("/api/submission/v1/**")
        // )
        .csrf(csrf -> csrf.disable())
        // Enable OAuth2 Resource Server with JWT
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
        )
        .httpBasic();  // must be last in this chain

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

