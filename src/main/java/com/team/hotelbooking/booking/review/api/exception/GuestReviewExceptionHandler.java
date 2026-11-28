package com.team.hotelbooking.booking.review.api.exception;

import com.team.hotelbooking.booking.review.domain.exception.GuestReviewException;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class GuestReviewExceptionHandler {
  private final ProblemDetailFactory problems;

  @ExceptionHandler(GuestReviewException.class)
  public ResponseEntity<ProblemDetail> handle(GuestReviewException error, WebRequest request) {
    HttpStatus status =
        switch (error.code()) {
          case VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
          case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
          case BOOKING_NOT_FOUND, HOTEL_NOT_FOUND, GUEST_REVIEW_NOT_FOUND -> HttpStatus.NOT_FOUND;
          case BOOKING_NOT_REVIEWABLE, GUEST_REVIEW_ALREADY_EXISTS, GUEST_REVIEW_VERSION_CONFLICT ->
              HttpStatus.CONFLICT;
        };
    return ResponseEntity.status(status)
        .body(problems.create(status, error.code(), error.getMessage(), request));
  }
}
