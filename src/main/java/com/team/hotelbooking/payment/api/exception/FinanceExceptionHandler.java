package com.team.hotelbooking.payment.api.exception;

import com.team.hotelbooking.payment.domain.exception.FinanceException;
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
public class FinanceExceptionHandler {

  private final ProblemDetailFactory problems;

  @ExceptionHandler(FinanceException.class)
  public ResponseEntity<ProblemDetail> handle(FinanceException exception, WebRequest request) {
    HttpStatus status =
        switch (exception.code()) {
          case INVALID_CONTRACT_PERIOD, INVALID_COMMISSION_RATE -> HttpStatus.BAD_REQUEST;
          case CONTRACT_CODE_ALREADY_EXISTS,
              ACTIVE_CONTRACT_PERIOD_OVERLAP,
              CONCURRENT_MODIFICATION_DETECTED ->
              HttpStatus.CONFLICT;
          case HOTEL_NOT_FOUND, USER_NOT_FOUND, FINANCIAL_ACCESS_NOT_FOUND -> HttpStatus.NOT_FOUND;
          case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
        };

    ProblemDetail body = problems.create(status, exception.code(), exception.getMessage(), request);
    return ResponseEntity.status(status).body(body);
  }
}
