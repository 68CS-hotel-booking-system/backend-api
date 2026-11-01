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
          case VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;

          case EMAIL_ALREADY_USED, PHONE_ALREADY_USED, CONFLICT -> HttpStatus.CONFLICT;

          case ACCOUNT_LOCKED -> HttpStatus.FORBIDDEN;

          case EMAIL_NOT_VERIFIED -> HttpStatus.FORBIDDEN;

          case INVALID_CREDENTIALS -> HttpStatus.UNAUTHORIZED;

          case TOKEN_INVALID, TOKEN_EXPIRED, TOKEN_ALREADY_USED -> HttpStatus.valueOf(422);

          case RATE_LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;
        };

    ProblemDetail body = problems.create(status, exception.code(), exception.getMessage(), request);

    return ResponseEntity.status(status).body(body);
  }
}
