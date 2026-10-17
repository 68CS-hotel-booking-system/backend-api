package com.team.hotelbooking.shared.web;

import com.team.hotelbooking.shared.exception.ErrorCode;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

@Component
public class ProblemDetailFactory {

  private static final String TRACE_ID_ATTRIBUTE =
      ProblemDetailFactory.class.getName() + ".traceId";

  public ProblemDetail create(HttpStatusCode status, ErrorCode errorCode, WebRequest request) {
    return create(status, errorCode, errorCode.defaultMessage(), List.of(), request);
  }

  public ProblemDetail create(
      HttpStatusCode status, ErrorCode errorCode, String detail, WebRequest request) {
    return create(status, errorCode, detail, List.of(), request);
  }

  public ProblemDetail create(
      HttpStatusCode status,
      ErrorCode errorCode,
      String detail,
      List<FieldErrorResponse> errors,
      WebRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setProperty("code", errorCode.code());
    problem.setProperty("traceId", traceId(request));
    problem.setProperty("errors", List.copyOf(errors));
    if (request instanceof ServletWebRequest servletRequest) {
      problem.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
    }
    return problem;
  }

  /** Reuses the active tracing ID, or generates a correlation ID stable for this request. */
  public String traceId(WebRequest request) {
    Object existing = request.getAttribute(TRACE_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
    if (existing instanceof String traceId) {
      return traceId;
    }
    String traceId = MDC.get("traceId");
    if (traceId == null || traceId.isBlank()) {
      traceId = UUID.randomUUID().toString().replace("-", "");
    }
    request.setAttribute(TRACE_ID_ATTRIBUTE, traceId, RequestAttributes.SCOPE_REQUEST);
    return traceId;
  }
}
