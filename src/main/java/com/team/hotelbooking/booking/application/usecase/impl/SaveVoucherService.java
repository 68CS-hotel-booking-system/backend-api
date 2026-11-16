package com.team.hotelbooking.booking.application.usecase.impl;

import com.team.hotelbooking.booking.application.command.SaveVoucherCommand;
import com.team.hotelbooking.booking.application.port.BookingHotelPort;
import com.team.hotelbooking.booking.application.port.BookingIdentityPort;
import com.team.hotelbooking.booking.application.result.SaveVoucherResult;
import com.team.hotelbooking.booking.application.usecase.SaveVoucherUseCase;
import com.team.hotelbooking.booking.domain.exception.BookingErrorCode;
import com.team.hotelbooking.booking.domain.exception.BookingException;
import com.team.hotelbooking.booking.domain.model.Voucher;
import com.team.hotelbooking.booking.domain.model.Voucher.DiscountType;
import com.team.hotelbooking.booking.domain.model.Voucher.FundingSource;
import com.team.hotelbooking.booking.domain.model.Voucher.Status;
import com.team.hotelbooking.booking.domain.repository.VoucherRepository;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class SaveVoucherService implements SaveVoucherUseCase {

  private final BookingIdentityPort accounts;
  private final BookingHotelPort hotels;
  private final VoucherRepository vouchers;
  private final Clock clock;

  public SaveVoucherService(
      BookingIdentityPort accounts,
      BookingHotelPort hotels,
      VoucherRepository vouchers,
      Clock clock) {
    this.accounts = accounts;
    this.hotels = hotels;
    this.vouchers = vouchers;
    this.clock = clock;
  }

  @Override
  public SaveVoucherResult execute(SaveVoucherCommand command) {
    var actor =
        accounts
            .findById(command.actorId())
            .filter(BookingIdentityPort.AccountSnapshot::active)
            .orElseThrow(() -> new BookingException(BookingErrorCode.ACCESS_DENIED));
    boolean admin = actor.roles().contains("ADMIN");
    UUID hotelId = command.hotelId();
    if (hotelId == null ? !admin : !hotels.canManageVouchers(hotelId, actor.id(), admin)) {
      throw new BookingException(BookingErrorCode.ACCESS_DENIED);
    }
    FundingSource funding =
        command.fundingSource() == null
            ? (hotelId == null ? FundingSource.PLATFORM : FundingSource.HOTEL)
            : parse(FundingSource.class, command.fundingSource());
    if (!admin && funding != FundingSource.HOTEL) {
      throw new BookingException(BookingErrorCode.ACCESS_DENIED);
    }

    String code = Voucher.normalizeCode(command.code());
    UUID id = UUID.randomUUID();
    if (!command.create()) {
      Voucher existing =
          vouchers
              .findByCode(code, true)
              .filter(voucher -> Objects.equals(hotelId, voucher.hotelId()))
              .orElseThrow(() -> new BookingException(BookingErrorCode.VOUCHER_NOT_FOUND));
      if (!admin && existing.fundingSource() != FundingSource.HOTEL) {
        throw new BookingException(BookingErrorCode.ACCESS_DENIED);
      }
      id = existing.id();
    }
    Voucher voucher =
        new Voucher(
            id,
            hotelId,
            code,
            command.name(),
            parse(DiscountType.class, command.discountType()),
            command.percentageOff(),
            command.fixedAmount(),
            command.maxDiscountAmount(),
            Objects.requireNonNullElse(command.minBookingAmount(), 0L),
            command.usageLimit(),
            Objects.requireNonNullElse(command.perUserLimit(), 1),
            command.startsAt(),
            command.endsAt(),
            command.status() == null ? Status.DRAFT : parse(Status.class, command.status()),
            funding);
    voucher.validateConfiguration();
    vouchers.save(voucher, clock.instant(), command.create());
    return new SaveVoucherResult(id, hotelId, code, voucher.status().name());
  }

  private static <T extends Enum<T>> T parse(Class<T> type, String value) {
    if (value != null) {
      try {
        return Enum.valueOf(type, value);
      } catch (IllegalArgumentException ignored) {
        // Dùng mã lỗi nghiệp vụ cho cả lời gọi ngoài HTTP.
      }
    }
    throw new BookingException(
        BookingErrorCode.VALIDATION_FAILED, "Cấu hình voucher không hợp lệ.");
  }
}
