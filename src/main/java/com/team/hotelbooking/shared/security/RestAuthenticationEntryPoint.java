package com.team.hotelbooking.shared.security;

import com.team.hotelbooking.shared.exception.CommonErrorCode;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletWebRequest;
import tools.jackson.databind.json.JsonMapper;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ProblemDetailFactory problemDetailFactory;
  private final JsonMapper jsonMapper;

  public RestAuthenticationEntryPoint(
      ProblemDetailFactory problemDetailFactory, JsonMapper jsonMapper) {
    this.problemDetailFactory = problemDetailFactory;
    this.jsonMapper = jsonMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    var problem =
        problemDetailFactory.create(
            HttpStatus.UNAUTHORIZED, CommonErrorCode.UNAUTHORIZED, new ServletWebRequest(request));

    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    // Phù hợp với HTTP Basic trong ví dụ này.
    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"hotel-booking\"");

    response.getOutputStream().write(jsonMapper.writeValueAsBytes(problem));
  }
}
