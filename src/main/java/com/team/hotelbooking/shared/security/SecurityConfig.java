package com.team.hotelbooking.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      RestAuthenticationEntryPoint entryPoint,
      RestAccessDeniedHandler deniedHandler,
      Converter<Jwt, AbstractAuthenticationToken> jwtConverter)
      throws Exception {

    http.authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(
                        "/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/auth/register",
                        "/api/v1/auth/email-verifications",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/forgot-password",
                        "/api/v1/auth/reset-password")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/hotels/*/availability")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/hotels/*/quote")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/cities",
                        "/api/v1/amenities",
                        "/api/v1/hotels/*",
                        "/api/v1/hotels",
                        "/api/v1/hotels/**",
                        "/api/v1/hotels/*/room-types")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/admin/roles",
                        "/api/v1/admin/users",
                        "/api/v1/admin/users/*/roles")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/admin/users",
                        "/api/v1/admin/users/*/email-verification-requests")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        HttpMethod.PUT,
                        "/api/v1/admin/users/*/roles/*",
                        "/api/v1/admin/users/*/status")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/admin/users/*/roles/*")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        HttpMethod.POST, "/api/v1/admin/hotels", "/api/v1/management/hotels")
                    .hasAnyRole("ADMIN", "HOTEL_OWNER")
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/management/hotels/*/room-types",
                        "/api/v1/management/hotels/*/rooms")
                    .hasAnyRole("ADMIN", "HOTEL_OWNER")
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/management/hotels",
                        "/api/v1/management/hotels/*",
                        "/api/v1/management/hotels/*/room-types",
                        "/api/v1/management/hotels/*/rooms",
                        "/api/v1/management/hotels/*/bookings",
                        "/api/v1/management/hotels/*/bookings/*")
                    .hasAnyRole("ADMIN", "HOTEL_OWNER")
                    .requestMatchers(HttpMethod.GET, "/api/v1/media/*/access")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .csrf(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable)
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                    .authenticationEntryPoint(entryPoint)
                    .accessDeniedHandler(deniedHandler))
        .exceptionHandling(
            exceptions ->
                exceptions.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler));

    return http.build();
  }
}
