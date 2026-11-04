package com.team.hotelbooking.hotel.api.exception;

import com.team.hotelbooking.hotel.domain.exception.HotelException;
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
public class HotelExceptionHandler {

  private final ProblemDetailFactory problems;

  @ExceptionHandler(HotelException.class)
  public ResponseEntity<ProblemDetail> handle(HotelException exception, WebRequest request) {
    HttpStatus status =
        switch (exception.getErrorCode()) {
          case INVALID_OCCUPANCY, INVALID_PRICE, INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
          case HOTEL_NOT_FOUND, ROOM_TYPE_NOT_FOUND, ROOM_NOT_FOUND -> HttpStatus.NOT_FOUND;
          case DUPLICATE_ROOM_NUMBER -> HttpStatus.CONFLICT;
        };

    ProblemDetail body =
        problems.create(status, exception.getErrorCode(), exception.getMessage(), request);
    return ResponseEntity.status(status).body(body);
  }
}
