package com.team.hotelbooking.shared.security;

import com.team.hotelbooking.shared.exception.CommonErrorCode;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletWebRequest;
import tools.jackson.databind.json.JsonMapper;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

  private final ProblemDetailFactory problemDetailFactory;
  private final JsonMapper jsonMapper;

  public RestAccessDeniedHandler(ProblemDetailFactory problemDetailFactory, JsonMapper jsonMapper) {
    this.problemDetailFactory = problemDetailFactory;
    this.jsonMapper = jsonMapper;
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
      throws IOException {
    var problem =
        problemDetailFactory.create(
            HttpStatus.FORBIDDEN, CommonErrorCode.ACCESS_DENIED, new ServletWebRequest(request));

    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    response.getOutputStream().write(jsonMapper.writeValueAsBytes(problem));
  }
}
