package com.team.hotelbooking.booking.api.exception;

import com.team.hotelbooking.booking.domain.exception.BookingException;
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
public class BookingExceptionHandler {

  private final ProblemDetailFactory problems;

  @ExceptionHandler(BookingException.class)
  public ResponseEntity<ProblemDetail> handle(BookingException exception, WebRequest request) {
    HttpStatus status =
        switch (exception.code()) {
          case VALIDATION_FAILED, INVALID_DATE_RANGE, MAX_STAY_EXCEEDED, INVALID_OCCUPANCY ->
              HttpStatus.BAD_REQUEST;
          case ACCESS_DENIED, EMAIL_NOT_VERIFIED -> HttpStatus.FORBIDDEN;
          case HOTEL_NOT_FOUND, ROOM_TYPE_NOT_FOUND, BOOKING_NOT_FOUND -> HttpStatus.NOT_FOUND;
          case RATE_NOT_AVAILABLE, PRICE_CHANGED -> HttpStatus.CONFLICT;
        };

    ProblemDetail body = problems.create(status, exception.code(), exception.getMessage(), request);
    return ResponseEntity.status(status).body(body);
  }
}
