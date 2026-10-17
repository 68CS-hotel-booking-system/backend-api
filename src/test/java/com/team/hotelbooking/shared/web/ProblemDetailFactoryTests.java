package com.team.hotelbooking.shared.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.team.hotelbooking.shared.exception.CommonErrorCode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class ProblemDetailFactoryTests {

  private final ProblemDetailFactory factory = new ProblemDetailFactory();

  @Test
  void createsProblemWithDefaultMessageCodeAndRequestPath() {
    MockHttpServletRequest servletRequest = new MockHttpServletRequest("POST", "/api/v1/bookings");
    servletRequest.setQueryString("token=secret");
    ServletWebRequest request = new ServletWebRequest(servletRequest);

    ProblemDetail problem =
        factory.create(HttpStatus.CONFLICT, CommonErrorCode.CONCURRENT_UPDATE, request);

    assertEquals(409, problem.getStatus());
    assertEquals(CommonErrorCode.CONCURRENT_UPDATE.defaultMessage(), problem.getDetail());
    assertEquals("CONCURRENT_UPDATE", problem.getProperties().get("code"));
    assertEquals("/api/v1/bookings", problem.getInstance().toString());
    assertNotNull(problem.getProperties().get("traceId"));
  }

  @Test
  void fallbackTraceIdIsStableWithinRequestAndDistinctAcrossRequests() {
    ServletWebRequest first = new ServletWebRequest(new MockHttpServletRequest());
    ServletWebRequest second = new ServletWebRequest(new MockHttpServletRequest());

    String traceId = factory.traceId(first);

    assertTrue(traceId.matches("[a-f0-9]{32}"));
    assertEquals(traceId, factory.traceId(first));
    assertNotEquals(traceId, factory.traceId(second));
  }

  @Test
  void reusesActiveTraceIdWithoutModifyingMdc() {
    String previous = MDC.get("traceId");
    MDC.put("traceId", "active-trace-id");
    try {
      ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest());

      ProblemDetail problem =
          factory.create(HttpStatus.BAD_REQUEST, CommonErrorCode.BAD_REQUEST, request);

      assertEquals("active-trace-id", problem.getProperties().get("traceId"));
      assertEquals("active-trace-id", MDC.get("traceId"));
    } finally {
      if (previous == null) {
        MDC.remove("traceId");
      } else {
        MDC.put("traceId", previous);
      }
    }
  }

  @Test
  void doesNotTrustClientSuppliedTraceId() {
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    servletRequest.addHeader("X-Trace-Id", "untrusted-client-value");

    String traceId = factory.traceId(new ServletWebRequest(servletRequest));

    assertNotEquals("untrusted-client-value", traceId);
  }

  @Test
  void snapshotsFieldErrors() {
    List<FieldErrorResponse> errors =
        new ArrayList<>(List.of(new FieldErrorResponse("name", "Required")));
    ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest());

    ProblemDetail problem =
        factory.create(
            HttpStatus.BAD_REQUEST,
            CommonErrorCode.VALIDATION_FAILED,
            CommonErrorCode.VALIDATION_FAILED.defaultMessage(),
            errors,
            request);
    errors.clear();

    assertEquals(
        List.of(new FieldErrorResponse("name", "Required")), problem.getProperties().get("errors"));
  }
}
