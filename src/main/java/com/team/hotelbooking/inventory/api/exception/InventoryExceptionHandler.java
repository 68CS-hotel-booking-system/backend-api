package com.team.hotelbooking.inventory.api.exception;

import com.team.hotelbooking.inventory.domain.exception.InventoryException;
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
public class InventoryExceptionHandler {

  private final ProblemDetailFactory problems;

  @ExceptionHandler(InventoryException.class)
  public ResponseEntity<ProblemDetail> handle(InventoryException exception, WebRequest request) {
    HttpStatus status =
        switch (exception.code()) {
          case INVALID_DATE_RANGE, MAX_STAY_EXCEEDED -> HttpStatus.BAD_REQUEST;
          case HOTEL_NOT_FOUND -> HttpStatus.NOT_FOUND;
        };

    ProblemDetail body = problems.create(status, exception.code(), exception.getMessage(), request);
    return ResponseEntity.status(status).body(body);
  }
}
