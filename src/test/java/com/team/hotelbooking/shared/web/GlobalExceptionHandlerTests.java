package com.team.hotelbooking.shared.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.hotelbooking.shared.exception.BusinessRuleException;
import com.team.hotelbooking.shared.exception.ConflictException;
import com.team.hotelbooking.shared.exception.ErrorCode;
import com.team.hotelbooking.shared.exception.NotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTests {

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(new SampleController())
            .setControllerAdvice(new GlobalExceptionHandler(new ProblemDetailFactory()))
            .build();
  }

  @Test
  void serializesSuccessEnvelope() throws Exception {
    mvc.perform(get("/test/success"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Thành công."))
        .andExpect(jsonPath("$.data").value("ready"));
  }

  @ParameterizedTest
  @CsvSource({"NOT_FOUND,404", "CONFLICT,409", "BUSINESS_RULE,422"})
  void mapsBusinessFailureWithoutHttpDependencyInException(String type, int expectedStatus)
      throws Exception {
    mvc.perform(get("/test/business/{type}", type))
        .andExpect(status().is(expectedStatus))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(expectedStatus))
        .andExpect(jsonPath("$.code").value("BOOKING_REJECTED"))
        .andExpect(jsonPath("$.detail").value("Không thể thực hiện yêu cầu đặt phòng."))
        .andExpect(jsonPath("$.instance").value("/test/business/" + type))
        .andExpect(jsonPath("$.traceId").isNotEmpty())
        .andExpect(jsonPath("$.errors").isEmpty());
  }

  @Test
  void supportsSafeCustomDomainMessages() throws Exception {
    mvc.perform(get("/test/custom-message"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("BOOKING_REJECTED"))
        .andExpect(jsonPath("$.detail").value("Không tìm thấy đặt phòng 123."));
  }

  @ParameterizedTest
  @ValueSource(strings = {"spring", "jpa"})
  void mapsOptimisticLockFailuresToConflict(String kind) throws Exception {
    mvc.perform(get("/test/concurrent/{kind}", kind))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONCURRENT_UPDATE"))
        .andExpect(jsonPath("$.traceId").isNotEmpty())
        .andExpect(content().string(not(containsString("sensitive database detail"))));
  }

  @Test
  void returnsFieldErrorsWithoutRejectedValues() throws Exception {
    mvc.perform(
            post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors[0].field").value("name"))
        .andExpect(jsonPath("$.errors[0].message").value("Tên không được để trống."))
        .andExpect(jsonPath("$.errors[0].rejectedValue").doesNotExist());
  }

  @Test
  void handlesMalformedJsonWithoutParserDetails() throws Exception {
    mvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
        .andExpect(content().string(not(containsString("Jackson"))));
  }

  @Test
  void handlesMissingRequestBody() throws Exception {
    mvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
  }

  @Test
  void handlesInvalidQueryParameterType() throws Exception {
    mvc.perform(get("/test/number").param("value", "not-a-number"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
  }

  @Test
  void handlesMissingQueryParameter() throws Exception {
    mvc.perform(get("/test/number"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
  }

  @Test
  void handlesControllerParameterConstraints() throws Exception {
    mvc.perform(get("/test/page").param("page", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors[0].field").value("page"));
  }

  @Test
  void handlesNestedBeanErrorsDuringMethodValidation() throws Exception {
    mvc.perform(
            post("/test/mixed")
                .param("page", "0")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors[0].field").value("name"));
  }

  @Test
  void retainsAllowHeaderOnUnsupportedMethod() throws Exception {
    mvc.perform(get("/test/body"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().string(HttpHeaders.ALLOW, containsString("POST")))
        .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
  }

  @Test
  void handlesUnsupportedContentType() throws Exception {
    mvc.perform(post("/test/body").contentType(MediaType.TEXT_PLAIN).content("name"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
  }

  @Test
  void handlesMissingEndpoint() throws Exception {
    mvc.perform(get("/missing"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void hidesUnexpectedExceptionDetails() throws Exception {
    mvc.perform(get("/test/unexpected"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
        .andExpect(jsonPath("$.traceId").isNotEmpty())
        .andExpect(jsonPath("$.errors").isEmpty())
        .andExpect(content().string(not(containsString("secret-password"))));
  }

  @Test
  void treatsReturnValueValidationAsServerFailure() throws Exception {
    mvc.perform(get("/test/invalid-response"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
        .andExpect(jsonPath("$.errors").isEmpty());
  }

  @Test
  void handlesAuthenticationFailureRaisedInMvc() throws Exception {
    mvc.perform(get("/test/unauthorized"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void handlesAuthorizationFailureRaisedInMvc() throws Exception {
    mvc.perform(get("/test/forbidden"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }

  @RestController
  @RequestMapping("/test")
  static class SampleController {

    @GetMapping("/success")
    ApiResponse<String> success() {
      return ApiResponse.success("ready");
    }

    @GetMapping("/business/{type}")
    void business(@PathVariable String type) {
      throw switch (type) {
        case "NOT_FOUND" -> new NotFoundException(SampleErrorCode.REJECTED);
        case "CONFLICT" -> new ConflictException(SampleErrorCode.REJECTED);
        default -> new BusinessRuleException(SampleErrorCode.REJECTED);
      };
    }

    @GetMapping("/custom-message")
    void customMessage() {
      throw new NotFoundException(SampleErrorCode.REJECTED, "Không tìm thấy đặt phòng 123.");
    }

    @GetMapping("/concurrent/{kind}")
    void concurrent(@PathVariable String kind) {
      if (kind.equals("spring")) {
        throw new OptimisticLockingFailureException("sensitive database detail");
      }
      throw new OptimisticLockException("sensitive database detail");
    }

    @PostMapping("/body")
    void body(@Valid @RequestBody SampleRequest request) {}

    @GetMapping("/number")
    void number(@RequestParam int value) {}

    @GetMapping("/page")
    void page(@RequestParam @Min(0) int page) {}

    @PostMapping("/mixed")
    void mixed(@Valid @RequestBody SampleRequest request, @RequestParam @Min(0) int page) {}

    @GetMapping("/unexpected")
    void unexpected() {
      throw new IllegalStateException("secret-password");
    }

    @NotNull
    @GetMapping("/invalid-response")
    String invalidResponse() {
      return null;
    }

    @GetMapping("/unauthorized")
    void unauthorized() {
      throw new BadCredentialsException("sensitive authentication detail");
    }

    @GetMapping("/forbidden")
    void forbidden() {
      throw new AccessDeniedException("sensitive authorization detail");
    }
  }

  record SampleRequest(@NotBlank(message = "Tên không được để trống.") String name) {}

  enum SampleErrorCode implements ErrorCode {
    REJECTED;

    @Override
    public String code() {
      return "BOOKING_REJECTED";
    }

    @Override
    public String defaultMessage() {
      return "Không thể thực hiện yêu cầu đặt phòng.";
    }
  }
}
