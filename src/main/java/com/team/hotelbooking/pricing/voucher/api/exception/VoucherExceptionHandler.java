package com.team.hotelbooking.pricing.voucher.api.exception;

import com.team.hotelbooking.pricing.voucher.domain.exception.VoucherException;
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
public class VoucherExceptionHandler {
  private final ProblemDetailFactory problems;

  @ExceptionHandler(VoucherException.class)
  public ResponseEntity<ProblemDetail> handle(VoucherException e, WebRequest request) {
    var status =
        switch (e.code()) {
          case VOUCHER_ACCESS_DENIED -> HttpStatus.FORBIDDEN;
          case VOUCHER_NOT_FOUND -> HttpStatus.NOT_FOUND;
          case VOUCHER_INVALID -> HttpStatus.BAD_REQUEST;
          default -> HttpStatus.CONFLICT;
        };
    return ResponseEntity.status(status)
        .body(problems.create(status, e.code(), e.getMessage(), request));
  }
}
