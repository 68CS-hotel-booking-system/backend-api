package com.team.hotelbooking.booking.application.usecase.impl;

import com.team.hotelbooking.booking.application.command.CreateBookingCommand;
import com.team.hotelbooking.booking.application.command.CreateBookingCommand.ContactInput;
import com.team.hotelbooking.booking.application.port.BookingIdentityPort;
import com.team.hotelbooking.booking.application.port.BookingIdentityPort.AccountSnapshot;
import com.team.hotelbooking.booking.application.port.BookingInventoryPort;
import com.team.hotelbooking.booking.application.port.BookingPricingPort;
import com.team.hotelbooking.booking.application.port.BookingPricingPort.CalculatedStayQuote;
import com.team.hotelbooking.booking.application.port.BookingPricingPort.StayQuoteCriteria;
import com.team.hotelbooking.booking.application.port.BookingPricingPort.StayQuoteItemCriteria;
import com.team.hotelbooking.booking.application.result.BookingDetailResult;
import com.team.hotelbooking.booking.application.usecase.CreateBookingUseCase;
import com.team.hotelbooking.booking.domain.exception.BookingErrorCode;
import com.team.hotelbooking.booking.domain.exception.BookingException;
import com.team.hotelbooking.booking.domain.model.Booking;
import com.team.hotelbooking.booking.domain.model.Booking.BookingItem;
import com.team.hotelbooking.booking.domain.model.Booking.BookingNightPrice;
import com.team.hotelbooking.booking.domain.model.Booking.ContactSnapshot;
import com.team.hotelbooking.booking.domain.repository.BookingRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class CreateBookingService implements CreateBookingUseCase {

  private static final DateTimeFormatter CODE_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT);

  private final BookingRepository bookings;
  private final BookingIdentityPort accounts;
  private final BookingPricingPort pricing;
  private final BookingInventoryPort inventory;
  private final Clock clock;
  private final Duration holdTtl;

  public CreateBookingService(
      BookingRepository bookings,
      BookingIdentityPort accounts,
      BookingPricingPort pricing,
      BookingInventoryPort inventory,
      Clock clock,
      Duration holdTtl) {

    if (holdTtl == null || holdTtl.isZero() || holdTtl.isNegative()) {
      throw new IllegalArgumentException("Hold TTL must be positive.");
    }

    this.bookings = bookings;
    this.accounts = accounts;
    this.pricing = pricing;
    this.inventory = inventory;
    this.clock = clock;
    this.holdTtl = holdTtl;
  }

  @Override
  public BookingDetailResult execute(CreateBookingCommand command) {
    if (command == null
        || command.customerId() == null
        || command.hotelId() == null
        || command.items() == null
        || command.items().isEmpty()) {
      throw new BookingException(BookingErrorCode.VALIDATION_FAILED);
    }

    AccountSnapshot customer =
        accounts
            .findById(command.customerId())
            .orElseThrow(() -> new BookingException(BookingErrorCode.ACCESS_DENIED));

    if (!customer.active()) {
      throw new BookingException(BookingErrorCode.ACCESS_DENIED);
    }

    if (!customer.emailVerified()) {
      throw new BookingException(BookingErrorCode.EMAIL_NOT_VERIFIED);
    }

    ContactSnapshot booker = resolveBooker(command.booker(), customer);
    ContactSnapshot guest = resolveGuest(command.guest(), booker);

    List<StayQuoteItemCriteria> quoteItems =
        command.items().stream()
            .map(
                i ->
                    new StayQuoteItemCriteria(
                        i.roomTypeId(), i.quantity(), i.adults(), i.children()))
            .toList();

    CalculatedStayQuote quote =
        pricing.calculateQuote(
            new StayQuoteCriteria(
                command.hotelId(),
                command.checkIn(),
                command.checkOut(),
                command.expectedCheckInTime(),
                command.expectedCheckOutTime(),
                quoteItems));

    if (command.quotedTotalAmount() != null && command.quotedTotalAmount() != quote.totalAmount()) {
      throw new BookingException(BookingErrorCode.PRICE_CHANGED);
    }

    UUID bookingId = UUID.randomUUID();
    Instant now = clock.instant();
    Instant expiresAt = now.plus(holdTtl);
    String bookingCode = generateBookingCode(bookingId, now);

    int totalAdults =
        quote.items().stream().mapToInt(BookingPricingPort.CalculatedItemQuote::adults).sum();
    int totalChildren =
        quote.items().stream().mapToInt(BookingPricingPort.CalculatedItemQuote::children).sum();

    List<BookingItem> domainItems =
        quote.items().stream()
            .map(
                qi ->
                    new BookingItem(
                        UUID.randomUUID(),
                        qi.roomTypeId(),
                        qi.roomTypeName(),
                        qi.quantity(),
                        qi.adults(),
                        qi.children(),
                        qi.extraGuests(),
                        qi.roomSubtotalAmount(),
                        qi.earlyCheckInSurchargeAmount(),
                        qi.lateCheckOutSurchargeAmount(),
                        qi.extraGuestSurchargeAmount(),
                        qi.surchargeAmount(),
                        qi.subtotalAmount(),
                        qi.nightlyPrices().stream()
                            .map(np -> new BookingNightPrice(np.stayDate(), np.unitPrice()))
                            .toList()))
            .toList();

    Booking booking =
        Booking.createPendingPayment(
            bookingId,
            bookingCode,
            customer.id(),
            quote.hotelId(),
            booker,
            guest,
            quote.checkIn(),
            quote.checkOut(),
            quote.days(),
            quote.nights(),
            quote.standardCheckInTime(),
            quote.standardCheckOutTime(),
            quote.expectedCheckInTime(),
            quote.expectedCheckOutTime(),
            totalAdults,
            totalChildren,
            quote.currency(),
            quote.roomSubtotalAmount(),
            quote.earlyCheckInSurchargeAmount(),
            quote.lateCheckOutSurchargeAmount(),
            quote.extraGuestSurchargeAmount(),
            quote.surchargeAmount(),
            quote.subtotalAmount(),
            quote.discountAmount(),
            quote.totalAmount(),
            expiresAt,
            now,
            domainItems);

    Booking saved = bookings.add(booking);
    // Giữ phòng liên tục trong cùng transaction; hết phòng phải rollback cả đơn và giá chốt.
    inventory.hold(saved);
    return BookingDetailResult.from(saved);
  }

  private static ContactSnapshot resolveBooker(ContactInput input, AccountSnapshot customer) {
    if (input == null) {
      throw new BookingException(
          BookingErrorCode.VALIDATION_FAILED, "Thông tin liên hệ người đặt là bắt buộc.");
    }
    String name =
        (input.fullName() == null || input.fullName().isBlank())
            ? customer.fullName()
            : input.fullName();
    String email =
        (input.email() == null || input.email().isBlank()) ? customer.email() : input.email();
    return ContactSnapshot.create(name, email, input.phone(), "người đặt");
  }

  private static ContactSnapshot resolveGuest(ContactInput input, ContactSnapshot booker) {
    if (input == null
        || ((input.fullName() == null || input.fullName().isBlank())
            && (input.email() == null || input.email().isBlank())
            && (input.phone() == null || input.phone().isBlank()))) {
      return booker;
    }
    String name =
        (input.fullName() == null || input.fullName().isBlank())
            ? booker.fullName()
            : input.fullName();
    String email =
        (input.email() == null || input.email().isBlank()) ? booker.email() : input.email();
    String phone =
        (input.phone() == null || input.phone().isBlank()) ? booker.phone() : input.phone();
    return ContactSnapshot.create(name, email, phone, "người ở");
  }

  private static String generateBookingCode(UUID bookingId, Instant now) {
    String datePart = LocalDate.ofInstant(now, ZoneOffset.UTC).format(CODE_DATE_FORMAT);
    String suffix = bookingId.toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
    return "BK-" + datePart + "-" + suffix;
  }
}
