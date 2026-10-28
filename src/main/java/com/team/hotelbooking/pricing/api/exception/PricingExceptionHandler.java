package com.team.hotelbooking.pricing.api.exception;

import com.team.hotelbooking.pricing.domain.exception.PricingException;
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
public class PricingExceptionHandler {

  private final ProblemDetailFactory problems;

  @ExceptionHandler(PricingException.class)
  public ResponseEntity<ProblemDetail> handle(PricingException exception, WebRequest request) {
    HttpStatus status =
        switch (exception.code()) {
          case INVALID_DATE_RANGE, MAX_RANGE_EXCEEDED -> HttpStatus.BAD_REQUEST;
          case HOTEL_NOT_FOUND, ROOM_TYPE_NOT_FOUND -> HttpStatus.NOT_FOUND;
        };

    ProblemDetail body = problems.create(status, exception.code(), exception.getMessage(), request);
    return ResponseEntity.status(status).body(body);
  }
}
