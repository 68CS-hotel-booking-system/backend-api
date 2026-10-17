package com.team.hotelbooking.shared.web;

import com.team.hotelbooking.shared.exception.CommonErrorCode;
import com.team.hotelbooking.shared.exception.ConflictException;
import com.team.hotelbooking.shared.exception.DomainException;
import com.team.hotelbooking.shared.exception.NotFoundException;
import jakarta.persistence.OptimisticLockException;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private final ProblemDetailFactory problemDetailFactory;

  public GlobalExceptionHandler(ProblemDetailFactory problemDetailFactory) {
    this.problemDetailFactory = problemDetailFactory;
  }

  @ExceptionHandler(DomainException.class)
  public @Nullable ResponseEntity<Object> handleDomainException(
      DomainException exception, WebRequest request) {
    HttpStatus status =
        switch (exception) {
          case NotFoundException ignored -> HttpStatus.NOT_FOUND;
          case ConflictException ignored -> HttpStatus.CONFLICT;
          default -> HttpStatus.UNPROCESSABLE_CONTENT;
        };
    ProblemDetail problem =
        problemDetailFactory.create(
            status, exception.getErrorCode(), exception.getMessage(), request);
    return handleExceptionInternal(exception, problem, new HttpHeaders(), status, request);
  }

  @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
  public @Nullable ResponseEntity<Object> handleConcurrentUpdate(
      Exception exception, WebRequest request) {
    ProblemDetail problem =
        problemDetailFactory.create(
            HttpStatus.CONFLICT, CommonErrorCode.CONCURRENT_UPDATE, request);
    return handleExceptionInternal(
        exception, problem, new HttpHeaders(), HttpStatus.CONFLICT, request);
  }

  // These handlers cover MVC exceptions. Security filter failures need separate security handlers.
  @ExceptionHandler(AuthenticationException.class)
  public @Nullable ResponseEntity<Object> handleAuthenticationException(
      AuthenticationException exception, WebRequest request) {
    ProblemDetail problem =
        problemDetailFactory.create(HttpStatus.UNAUTHORIZED, CommonErrorCode.UNAUTHORIZED, request);
    return handleExceptionInternal(
        exception, problem, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public @Nullable ResponseEntity<Object> handleAccessDeniedException(
      AccessDeniedException exception, WebRequest request) {
    ProblemDetail problem =
        problemDetailFactory.create(HttpStatus.FORBIDDEN, CommonErrorCode.ACCESS_DENIED, request);
    return handleExceptionInternal(
        exception, problem, new HttpHeaders(), HttpStatus.FORBIDDEN, request);
  }

  @ExceptionHandler(Exception.class)
  public @Nullable ResponseEntity<Object> handleUnexpectedException(
      Exception exception, WebRequest request) {
    return handleExceptionInternal(
        exception, null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
  }

  @Override
  protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<FieldErrorResponse> errors =
        exception.getBindingResult().getAllErrors().stream().map(this::violation).toList();
    ProblemDetail problem = validationProblem(status, errors, request);
    return handleExceptionInternal(exception, problem, headers, status, request);
  }

  @Override
  protected @Nullable ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    if (exception.isForReturnValue()) {
      return handleExceptionInternal(exception, null, headers, status, request);
    }

    List<FieldErrorResponse> errors = new ArrayList<>();
    for (var result : exception.getParameterValidationResults()) {
      if (result instanceof ParameterErrors parameterErrors) {
        parameterErrors.getAllErrors().stream().map(this::violation).forEach(errors::add);
      } else {
        String name = result.getMethodParameter().getParameterName();
        String field =
            name != null ? name : "arg" + result.getMethodParameter().getParameterIndex();
        result
            .getResolvableErrors()
            .forEach(error -> errors.add(new FieldErrorResponse(field, message(error))));
      }
    }
    exception
        .getCrossParameterValidationResults()
        .forEach(error -> errors.add(new FieldErrorResponse("request", message(error))));

    return handleExceptionInternal(
        exception, validationProblem(status, errors, request), headers, status, request);
  }

  @Override
  protected @Nullable ResponseEntity<Object> handleExceptionInternal(
      Exception exception,
      @Nullable Object body,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ProblemDetail response;
    if (status.is5xxServerError()) {
      response = problemDetailFactory.create(status, CommonErrorCode.INTERNAL_ERROR, request);
      log.error(
          "Request failed: {} [traceId={}]",
          request.getDescription(false),
          problemDetailFactory.traceId(request),
          exception);
    } else if (body instanceof ProblemDetail supplied
        && supplied.getProperties() != null
        && supplied.getProperties().containsKey("code")) {
      response = supplied;
    } else if (exception instanceof HttpMessageNotReadableException) {
      response = problemDetailFactory.create(status, CommonErrorCode.MALFORMED_REQUEST, request);
    } else {
      response = problemDetailFactory.create(status, defaultCode(status), request);
    }
    if (!status.is5xxServerError()) {
      log.debug(
          "Request rejected: {} [code={}, traceId={}]",
          request.getDescription(false),
          response.getProperties().get("code"),
          problemDetailFactory.traceId(request));
    }
    // Retain Spring's protocol headers (for example Allow on a 405 response).
    return super.handleExceptionInternal(exception, response, headers, status, request);
  }

  private ProblemDetail validationProblem(
      HttpStatusCode status, List<FieldErrorResponse> errors, WebRequest request) {
    return problemDetailFactory.create(
        status,
        CommonErrorCode.VALIDATION_FAILED,
        CommonErrorCode.VALIDATION_FAILED.defaultMessage(),
        errors,
        request);
  }

  private FieldErrorResponse violation(ObjectError error) {
    String field =
        error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
    return new FieldErrorResponse(field, message(error));
  }

  private String message(MessageSourceResolvable error) {
    String message = error.getDefaultMessage();
    return message != null ? message : "Giá trị không hợp lệ.";
  }

  private CommonErrorCode defaultCode(HttpStatusCode status) {
    return switch (status.value()) {
      case 400 -> CommonErrorCode.BAD_REQUEST;
      case 401 -> CommonErrorCode.UNAUTHORIZED;
      case 403 -> CommonErrorCode.ACCESS_DENIED;
      case 404 -> CommonErrorCode.RESOURCE_NOT_FOUND;
      case 405 -> CommonErrorCode.METHOD_NOT_ALLOWED;
      case 406 -> CommonErrorCode.NOT_ACCEPTABLE;
      case 409 -> CommonErrorCode.CONFLICT;
      case 413 -> CommonErrorCode.PAYLOAD_TOO_LARGE;
      case 415 -> CommonErrorCode.UNSUPPORTED_MEDIA_TYPE;
      default -> CommonErrorCode.REQUEST_FAILED;
    };
  }
}
