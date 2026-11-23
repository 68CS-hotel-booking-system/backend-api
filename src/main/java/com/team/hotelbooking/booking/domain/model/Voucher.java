package com.team.hotelbooking.booking.domain.model;

import com.team.hotelbooking.booking.domain.exception.BookingErrorCode;
import com.team.hotelbooking.booking.domain.exception.BookingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record Voucher(
    UUID id,
    UUID hotelId,
    String code,
    String name,
    DiscountType discountType,
    BigDecimal percentageOff,
    Long fixedAmount,
    Long maxDiscountAmount,
    long minBookingAmount,
    Integer usageLimit,
    int perUserLimit,
    Instant startsAt,
    Instant endsAt,
    Status status,
    FundingSource fundingSource) {

  public enum DiscountType {
    PERCENTAGE,
    FIXED_AMOUNT
  }

  public enum Status {
    DRAFT,
    ACTIVE,
    PAUSED,
    EXPIRED,
    ARCHIVED
  }

  public enum FundingSource {
    PLATFORM,
    HOTEL
  }

  public static String normalizeCode(String code) {
    String normalized = code == null ? "" : code.strip().toUpperCase(Locale.ROOT);
    if (!normalized.matches("[A-Z0-9_-]{1,50}")) {
      throw new BookingException(BookingErrorCode.VOUCHER_INVALID);
    }
    return normalized;
  }

  public void validateConfiguration() {
    boolean validDiscount =
        discountType != null
            && switch (discountType) {
              case PERCENTAGE ->
                  percentageOff != null
                      && percentageOff.signum() > 0
                      && percentageOff.compareTo(BigDecimal.valueOf(100)) <= 0
                      && percentageOff.stripTrailingZeros().scale() <= 2
                      && fixedAmount == null;
              case FIXED_AMOUNT -> fixedAmount != null && fixedAmount > 0 && percentageOff == null;
            };
    if (!validDiscount
        || name == null
        || name.isBlank()
        || name.length() > 150
        || (maxDiscountAmount != null && maxDiscountAmount <= 0)
        || minBookingAmount < 0
        || (usageLimit != null && usageLimit <= 0)
        || perUserLimit <= 0
        || startsAt == null
        || endsAt == null
        || !endsAt.isAfter(startsAt)
        || status == null
        || fundingSource == null
        || (fundingSource == FundingSource.HOTEL && hotelId == null)) {
      throw new BookingException(
          BookingErrorCode.VALIDATION_FAILED, "Cấu hình voucher không hợp lệ.");
    }
  }

  public long discountFor(UUID bookingHotelId, long subtotal, Instant now) {
    if (status != Status.ACTIVE
        || (hotelId != null && !hotelId.equals(bookingHotelId))
        || now.isBefore(startsAt)
        || !now.isBefore(endsAt)
        || subtotal < minBookingAmount) {
      throw new BookingException(BookingErrorCode.VOUCHER_INVALID);
    }

    long discount =
        discountType == DiscountType.FIXED_AMOUNT
            ? fixedAmount
            : BigDecimal.valueOf(subtotal)
                .multiply(percentageOff)
                .movePointLeft(2)
                .setScale(0, RoundingMode.DOWN)
                .longValueExact();
    if (maxDiscountAmount != null) {
      discount = Math.min(discount, maxDiscountAmount);
    }
    if (discount <= 0 || discount >= subtotal) {
      throw new BookingException(
          BookingErrorCode.VOUCHER_INVALID,
          "Voucher phải tạo khoản giảm dương và tổng tiền cần trả phải lớn hơn 0.");
    }
    return discount;
  }
}
