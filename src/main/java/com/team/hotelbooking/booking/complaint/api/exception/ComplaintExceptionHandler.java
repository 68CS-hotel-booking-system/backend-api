package com.team.hotelbooking.booking.complaint.api.exception;

import com.team.hotelbooking.booking.complaint.domain.exception.ComplaintException;
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
public class ComplaintExceptionHandler {

  private final ProblemDetailFactory problems;

  @ExceptionHandler(ComplaintException.class)
  public ResponseEntity<ProblemDetail> handle(ComplaintException exception, WebRequest request) {
    HttpStatus status =
        switch (exception.code()) {
          case VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
          case BOOKING_NOT_FOUND, COMPLAINT_NOT_FOUND -> HttpStatus.NOT_FOUND;
          case BOOKING_NOT_COMPLAINABLE, COMPLAINT_ALREADY_OPEN -> HttpStatus.CONFLICT;
        };

    ProblemDetail body = problems.create(status, exception.code(), exception.getMessage(), request);
    // FE dùng id này để mở khiếu nại đang xử lý thay vì gửi trùng.
    if (exception.openComplaintId() != null) {
      body.setProperty("openComplaintId", exception.openComplaintId());
    }
    return ResponseEntity.status(status).body(body);
  }
}
