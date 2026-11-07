package com.team.hotelbooking.identity.api.exception;

import com.team.hotelbooking.identity.domain.exception.IdentityException;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class IdentityExceptionHandler {

  private final ProblemDetailFactory problems;

  @ExceptionHandler(IdentityException.class)
  public ResponseEntity<ProblemDetail> handle(IdentityException exception, WebRequest request) {

    HttpStatus status =
        switch (exception.code()) {
          case VALIDATION_FAILED, CURRENT_PASSWORD_INCORRECT -> HttpStatus.BAD_REQUEST;

          case EMAIL_ALREADY_USED,
              PHONE_ALREADY_USED,
              CONFLICT,
              ROLE_UNAVAILABLE,
              CUSTOMER_ROLE_REMOVAL_FORBIDDEN,
              SELF_ADMIN_ROLE_REMOVAL_FORBIDDEN,
              STALE_USER_VERSION,
              LAST_ACTIVE_ADMIN_STATUS_CHANGE_FORBIDDEN,
              INVALID_USER_STATUS_TRANSITION,
              EMAIL_ALREADY_VERIFIED,
              LAST_ACTIVE_ADMIN ->
              HttpStatus.CONFLICT;

          case ACCOUNT_LOCKED, ACCESS_DENIED, SELF_STATUS_CHANGE_FORBIDDEN -> HttpStatus.FORBIDDEN;

          case USER_NOT_FOUND -> HttpStatus.NOT_FOUND;

          case EMAIL_NOT_VERIFIED -> HttpStatus.FORBIDDEN;

          case INVALID_CREDENTIALS,
              INVALID_REFRESH_TOKEN,
              REFRESH_TOKEN_REUSED,
              ACCOUNT_UNAVAILABLE ->
              HttpStatus.UNAUTHORIZED;

          case TOKEN_INVALID, TOKEN_EXPIRED, TOKEN_ALREADY_USED, NEW_PASSWORD_SAME_AS_CURRENT ->
              HttpStatus.valueOf(422);

          case RATE_LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;
          case NOT_IMPLEMENTED -> HttpStatus.NOT_IMPLEMENTED;
        };

    ProblemDetail body = problems.create(status, exception.code(), exception.getMessage(), request);

    return ResponseEntity.status(status).body(body);
  }
}
