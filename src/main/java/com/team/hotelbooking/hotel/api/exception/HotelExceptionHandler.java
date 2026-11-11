package com.team.hotelbooking.hotel.api.exception;

import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.shared.web.FieldErrorResponse;
import com.team.hotelbooking.shared.web.ProblemDetailFactory;
import java.util.List;
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
        switch (exception.code()) {
          case VALIDATION_FAILED,
              INVALID_OCCUPANCY,
              INVALID_PRICE,
              INVALID_REQUEST,
              AMENITY_CATEGORY_MISMATCH ->
              HttpStatus.BAD_REQUEST;
          case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
          case OWNER_NOT_FOUND,
              CITY_NOT_FOUND,
              HOTEL_NOT_FOUND,
              ROOM_TYPE_NOT_FOUND,
              ROOM_NOT_FOUND,
              AMENITY_NOT_FOUND ->
              HttpStatus.NOT_FOUND;
          case DUPLICATE_CUSTOM_AMENITY,
              AMENITY_CONCURRENT_UPDATE,
              OWNER_NOT_ELIGIBLE,
              ROOM_OPERATION_CONFLICT,
              HOTEL_SLUG_CONFLICT,
              ROOM_TYPE_INACTIVE,
              ROOM_NUMBER_CONFLICT,
              DUPLICATE_ROOM_NUMBER,
              INVALID_STATUS_TRANSITION,
              HOTEL_LOCKED ->
              HttpStatus.CONFLICT;
          case HOTEL_NOT_READY -> HttpStatus.UNPROCESSABLE_CONTENT;
        };

    // 422 HOTEL_NOT_READY trả từng điều kiện HT-03 còn thiếu trong errors, cùng dạng lỗi
    // validation.
    List<FieldErrorResponse> errors =
        exception.unmetRequirements().stream()
            .map(requirement -> new FieldErrorResponse(requirement.field(), requirement.message()))
            .toList();
    ProblemDetail body =
        problems.create(status, exception.code(), exception.getMessage(), errors, request);

    return ResponseEntity.status(status).body(body);
  }
}
