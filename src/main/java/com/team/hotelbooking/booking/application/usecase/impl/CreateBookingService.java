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
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class CreateBookingService implements CreateBookingUseCase {

  private static final DateTimeFormatter CODE_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT);

  private final com.team.hotelbooking.booking.application.port.BookingVoucherPort vouchers;
  private final BookingRepository bookings;
  private final BookingIdentityPort accounts;
  private final BookingPricingPort pricing;
  private final BookingInventoryPort inventory;
  private final Clock clock;
  private final Duration holdTtl;
  private final com.team.hotelbooking.booking.BookingCommissionCapture commissions;

  public CreateBookingService(
      BookingRepository bookings,
      BookingIdentityPort accounts,
      BookingPricingPort pricing,
      BookingInventoryPort inventory,
      Clock clock,
      Duration holdTtl,
      com.team.hotelbooking.booking.BookingCommissionCapture commissions) {

    this(
        bookings,
        accounts,
        pricing,
        inventory,
        clock,
        holdTtl,
        commissions,
        com.team.hotelbooking.booking.application.port.BookingVoucherPort.NONE);
  }

  public CreateBookingService(
      BookingRepository bookings,
      BookingIdentityPort accounts,
      BookingPricingPort pricing,
      BookingInventoryPort inventory,
      Clock clock,
      Duration holdTtl,
      com.team.hotelbooking.booking.BookingCommissionCapture commissions,
      com.team.hotelbooking.booking.application.port.BookingVoucherPort vouchers) {
    this.vouchers = vouchers;
    if (holdTtl == null || holdTtl.isZero() || holdTtl.isNegative()) {
      throw new IllegalArgumentException("Hold TTL must be positive.");
    }

    this.bookings = bookings;
    this.accounts = accounts;
    this.pricing = pricing;
    this.inventory = inventory;
    this.clock = clock;
    this.holdTtl = holdTtl;
    this.commissions = commissions;
  }

  @Override
  public BookingDetailResult execute(CreateBookingCommand command) {
    validateCommand(command);
    String idempotencyKey = Booking.validateIdempotencyKey(command.idempotencyKey());

    AccountSnapshot customer = loadVerifiedCustomer(command.customerId());
    ContactSnapshot booker = resolveBooker(command.booker(), customer);
    ContactSnapshot guest = resolveGuest(command.guest(), booker);

    if (idempotencyKey != null) {
      Optional<BookingDetailResult> existing =
          findIdempotentReplay(command, customer.id(), idempotencyKey, booker, guest);
      if (existing.isPresent()) {
        return existing.get();
      }
    }

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
                quoteItems,
                command.voucherCode()));

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
            domainItems,
            idempotencyKey);

    Booking saved = bookings.add(booking);
    vouchers.reserve(
        saved.hotelId(),
        command.voucherCode(),
        saved.id(),
        quote.roomSubtotalAmount(),
        quote.discountAmount(),
        expiresAt);
    try {
      commissions.capture(
          saved.id(),
          saved.hotelId(),
          quote.roomSubtotalAmount(),
          quote.discountAmount(),
          quote.totalAmount(),
          now);
    } catch (
        com.team.hotelbooking.booking.BookingCommissionCapture.ContractUnavailableException error) {
      throw new BookingException(BookingErrorCode.CONTRACT_NOT_EFFECTIVE);
    }
    // Giữ phòng liên tục trong cùng transaction; hết phòng phải rollback cả đơn và giá chốt.
    inventory.hold(saved);
    return BookingDetailResult.from(saved);
  }

  public Optional<BookingDetailResult> resolveIdempotentReplay(CreateBookingCommand command) {
    validateCommand(command);
    String idempotencyKey = Booking.validateIdempotencyKey(command.idempotencyKey());
    if (idempotencyKey == null) {
      return Optional.empty();
    }
    AccountSnapshot customer = loadVerifiedCustomer(command.customerId());
    ContactSnapshot booker = resolveBooker(command.booker(), customer);
    ContactSnapshot guest = resolveGuest(command.guest(), booker);
    return findIdempotentReplay(command, customer.id(), idempotencyKey, booker, guest);
  }

  private void validateCommand(CreateBookingCommand command) {
    if (command == null
        || command.customerId() == null
        || command.hotelId() == null
        || command.items() == null
        || command.items().isEmpty()
        || command.items().stream().anyMatch(i -> i == null || i.roomTypeId() == null)) {
      throw new BookingException(BookingErrorCode.VALIDATION_FAILED);
    }
  }

  private AccountSnapshot loadVerifiedCustomer(UUID customerId) {
    AccountSnapshot customer =
        accounts
            .findById(customerId)
            .orElseThrow(() -> new BookingException(BookingErrorCode.ACCESS_DENIED));

    if (!customer.active()) {
      throw new BookingException(BookingErrorCode.ACCESS_DENIED);
    }

    if (customer.roles().contains("ADMIN")) {
      throw new BookingException(BookingErrorCode.ADMIN_BOOKING_FORBIDDEN);
    }

    if (!customer.emailVerified()) {
      throw new BookingException(BookingErrorCode.EMAIL_NOT_VERIFIED);
    }
    return customer;
  }

  private Optional<BookingDetailResult> findIdempotentReplay(
      CreateBookingCommand command,
      UUID customerId,
      String idempotencyKey,
      ContactSnapshot booker,
      ContactSnapshot guest) {
    Optional<Booking> existingOpt =
        bookings.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey);
    if (existingOpt.isEmpty()) {
      return Optional.empty();
    }
    Booking existing = existingOpt.get();
    String requestedCode =
        command.voucherCode() == null || command.voucherCode().isBlank()
            ? null
            : command.voucherCode().trim().toUpperCase(Locale.ROOT);
    if (!Objects.equals(requestedCode, vouchers.bookingCode(existing.id()))
        || !matchesIntent(command, booker, guest, existing)) {
      throw new BookingException(BookingErrorCode.IDEMPOTENCY_CONFLICT);
    }
    return Optional.of(BookingDetailResult.from(existing));
  }

  private static boolean matchesIntent(
      CreateBookingCommand command,
      ContactSnapshot booker,
      ContactSnapshot guest,
      Booking existing) {
    if (!Objects.equals(command.hotelId(), existing.hotelId())
        || !Objects.equals(command.checkIn(), existing.checkIn())
        || !Objects.equals(command.checkOut(), existing.checkOut())) {
      return false;
    }

    LocalTime expectedCheckIn =
        command.expectedCheckInTime() != null
            ? command.expectedCheckInTime()
            : existing.standardCheckInTime();
    LocalTime expectedCheckOut =
        command.expectedCheckOutTime() != null
            ? command.expectedCheckOutTime()
            : existing.standardCheckOutTime();
    if (!Objects.equals(expectedCheckIn, existing.expectedCheckInTime())
        || !Objects.equals(expectedCheckOut, existing.expectedCheckOutTime())) {
      return false;
    }

    if (!Objects.equals(booker, existing.booker()) || !Objects.equals(guest, existing.guest())) {
      return false;
    }

    if (command.quotedTotalAmount() != null
        && command.quotedTotalAmount() != existing.totalAmount()) {
      return false;
    }

    List<ItemIntent> requestedItems =
        command.items().stream()
            .map(i -> new ItemIntent(i.roomTypeId(), i.quantity(), i.adults(), i.children()))
            .sorted()
            .toList();
    List<ItemIntent> storedItems =
        existing.items().stream()
            .map(i -> new ItemIntent(i.roomTypeId(), i.quantity(), i.adults(), i.children()))
            .sorted()
            .toList();
    return requestedItems.equals(storedItems);
  }

  private record ItemIntent(UUID roomTypeId, int quantity, int adults, int children)
      implements Comparable<ItemIntent> {
    @Override
    public int compareTo(ItemIntent other) {
      int cmp = this.roomTypeId.compareTo(other.roomTypeId);
      if (cmp != 0) {
        return cmp;
      }
      cmp = Integer.compare(this.quantity, other.quantity);
      if (cmp != 0) {
        return cmp;
      }
      cmp = Integer.compare(this.adults, other.adults);
      if (cmp != 0) {
        return cmp;
      }
      return Integer.compare(this.children, other.children);
    }
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
