package com.team.hotelbooking.media.api.exception;

import com.team.hotelbooking.media.application.port.ObjectStorageException;
import com.team.hotelbooking.media.domain.exception.MediaException;
import com.team.hotelbooking.shared.exception.CommonErrorCode;
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
public class MediaExceptionHandler {

  private final ProblemDetailFactory problems;

  @ExceptionHandler(MediaException.class)
  public ResponseEntity<ProblemDetail> handle(MediaException exception, WebRequest request) {

    HttpStatus status =
        switch (exception.code()) {
          case VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
          case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
          case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
          case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
          case PAYLOAD_TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE;
          case UNSUPPORTED_MEDIA_TYPE -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
          case MEDIA_NOT_READY, CONFLICT -> HttpStatus.CONFLICT;
        };

    return ResponseEntity.status(status)
        .body(problems.create(status, exception.code(), exception.getMessage(), request));
  }

  @ExceptionHandler(ObjectStorageException.class)
  public ResponseEntity<ProblemDetail> handleStorage(
      ObjectStorageException exception, WebRequest request) {

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            problems.create(
                HttpStatus.INTERNAL_SERVER_ERROR, CommonErrorCode.INTERNAL_ERROR, request));
  }
}
