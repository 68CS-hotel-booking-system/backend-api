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
                        "/api/v1/auth/login")
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
