package com.team.hotelbooking.shared.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DomainExceptionTests {

  @Test
  void retainsModuleErrorCodeAndUsesItsDefaultMessage() {
    DomainException exception = new NotFoundException(BookingErrorCode.NOT_FOUND);

    assertSame(BookingErrorCode.NOT_FOUND, exception.getErrorCode());
    assertEquals("BOOKING_NOT_FOUND", exception.getErrorCode().code());
    assertEquals("Không tìm thấy đặt phòng.", exception.getMessage());
  }

  @Test
  void customMessageDoesNotChangeErrorCode() {
    DomainException exception =
        new BusinessRuleException(BookingErrorCode.NOT_FOUND, "Thông báo cụ thể.");

    assertSame(BookingErrorCode.NOT_FOUND, exception.getErrorCode());
    assertEquals("Thông báo cụ thể.", exception.getMessage());
  }

  @Test
  void rejectsMissingErrorCode() {
    assertThrows(NullPointerException.class, () -> new ConflictException(null));
  }

  private enum BookingErrorCode implements ErrorCode {
    NOT_FOUND;

    @Override
    public String code() {
      return "BOOKING_NOT_FOUND";
    }

    @Override
    public String defaultMessage() {
      return "Không tìm thấy đặt phòng.";
    }
  }
}
