package com.team.hotelbooking.booking.application.usecase.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.team.hotelbooking.booking.application.command.CreateBookingCommand;
import com.team.hotelbooking.booking.application.command.CreateBookingCommand.BookingItemInput;
import com.team.hotelbooking.booking.application.command.CreateBookingCommand.ContactInput;
import com.team.hotelbooking.booking.application.command.GetCustomerBookingDetailQuery;
import com.team.hotelbooking.booking.application.command.GetManagementBookingDetailQuery;
import com.team.hotelbooking.booking.application.command.ListCustomerBookingsQuery;
import com.team.hotelbooking.booking.application.command.ListManagementBookingsQuery;
import com.team.hotelbooking.booking.application.port.BookingHotelPort;
import com.team.hotelbooking.booking.application.port.BookingHotelPort.HotelSnapshot;
import com.team.hotelbooking.booking.application.port.BookingIdentityPort;
import com.team.hotelbooking.booking.application.port.BookingIdentityPort.AccountSnapshot;
import com.team.hotelbooking.booking.application.port.BookingInventoryPort;
import com.team.hotelbooking.booking.application.port.BookingPricingPort;
import com.team.hotelbooking.booking.application.port.BookingPricingPort.CalculatedItemQuote;
import com.team.hotelbooking.booking.application.port.BookingPricingPort.CalculatedNightlyPrice;
import com.team.hotelbooking.booking.application.port.BookingPricingPort.CalculatedStayQuote;
import com.team.hotelbooking.booking.application.result.BookingDetailResult;
import com.team.hotelbooking.booking.application.result.BookingSummaryResult;
import com.team.hotelbooking.booking.application.usecase.CreateBookingUseCase;
import com.team.hotelbooking.booking.domain.exception.BookingErrorCode;
import com.team.hotelbooking.booking.domain.exception.BookingException;
import com.team.hotelbooking.booking.domain.model.Booking;
import com.team.hotelbooking.booking.domain.repository.BookingRepository;
import com.team.hotelbooking.booking.infrastructure.config.BookingConfig;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("Các dịch vụ đặt phòng, lưu giá/người đặt/người ở và truy vấn đơn")
class BookingServicesTests {

  private static final UUID CUSTOMER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
  private static final UUID OWNER_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
  private static final UUID HOTEL_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
  private static final UUID ROOM_TYPE_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");

  @Mock private BookingRepository bookings;
  @Mock private BookingIdentityPort accounts;
  @Mock private BookingHotelPort hotels;
  @Mock private BookingPricingPort pricing;
  @Mock private BookingInventoryPort inventory;

  private Clock clock;
  private CreateBookingService createBookingService;
  private ListCustomerBookingsService listCustomerBookingsService;
  private GetCustomerBookingDetailService getCustomerBookingDetailService;
  private ListManagementBookingsService listManagementBookingsService;
  private GetManagementBookingDetailService getManagementBookingDetailService;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(Instant.parse("2026-11-09T02:00:00Z"), ZoneOffset.UTC);
    createBookingService =
        new CreateBookingService(
            bookings, accounts, pricing, inventory, clock, Duration.ofMinutes(15));
    listCustomerBookingsService = new ListCustomerBookingsService(bookings, accounts, 50);
    getCustomerBookingDetailService = new GetCustomerBookingDetailService(bookings, accounts);
    listManagementBookingsService =
        new ListManagementBookingsService(bookings, accounts, hotels, 50);
    getManagementBookingDetailService =
        new GetManagementBookingDetailService(bookings, accounts, hotels);
  }

  private CalculatedStayQuote sampleQuote() {
    return new CalculatedStayQuote(
        HOTEL_ID,
        LocalDate.of(2026, 11, 10),
        LocalDate.of(2026, 11, 12),
        3,
        2,
        LocalTime.of(14, 0),
        LocalTime.of(12, 0),
        LocalTime.of(8, 0),
        LocalTime.of(12, 0),
        "VND",
        2_000_000L,
        0L,
        0L,
        0L,
        0L,
        2_000_000L,
        0L,
        2_000_000L,
        List.of(
            new CalculatedItemQuote(
                ROOM_TYPE_ID,
                "Deluxe King",
                1,
                2,
                1,
                0,
                2_000_000L,
                0L,
                0L,
                0L,
                0L,
                2_000_000L,
                List.of(
                    new CalculatedNightlyPrice(LocalDate.of(2026, 11, 10), 1_000_000L),
                    new CalculatedNightlyPrice(LocalDate.of(2026, 11, 11), 1_000_000L)))));
  }

  @Nested
  @DisplayName("CreateBookingUseCase.execute")
  class CreateBookingTests {

    @Test
    @DisplayName(
        "Lưu đầy đủ ngày/đêm, giờ nhận/trả, tiền phòng, phụ thu, người đặt/người ở và bàn giao thanh toán")
    void shouldCreateBookingWithBookerGuestAndPricingSnapshots() {
      // Given
      given(accounts.findById(CUSTOMER_ID))
          .willReturn(
              Optional.of(
                  new AccountSnapshot(
                      CUSTOMER_ID,
                      "Nguyễn Văn Đặt",
                      "booker@example.com",
                      true,
                      true,
                      Set.of("CUSTOMER"))));
      given(pricing.calculateQuote(any())).willReturn(sampleQuote());
      given(bookings.add(any(Booking.class))).willAnswer(invocation -> invocation.getArgument(0));

      CreateBookingCommand command =
          new CreateBookingCommand(
              CUSTOMER_ID,
              HOTEL_ID,
              LocalDate.of(2026, 11, 10),
              LocalDate.of(2026, 11, 12),
              LocalTime.of(8, 0),
              LocalTime.of(12, 0),
              new ContactInput("Nguyễn Văn Đặt", "booker@example.com", "0912345678"),
              new ContactInput("Trần Thị Ở", "guest@example.com", "0987654321"),
              List.of(new BookingItemInput(ROOM_TYPE_ID, 1, 2, 1)),
              2_000_000L);

      // When
      BookingDetailResult result = createBookingService.execute(command);

      // Then
      assertNotNull(result.id());
      assertNotNull(result.bookingCode());
      assertEquals(CUSTOMER_ID, result.customerId());
      assertEquals(HOTEL_ID, result.hotelId());
      assertEquals(3, result.days());
      assertEquals(2, result.nights());
      assertEquals("Nguyễn Văn Đặt", result.booker().fullName());
      assertEquals("booker@example.com", result.booker().email());
      assertEquals("0912345678", result.booker().phone());
      assertEquals("Trần Thị Ở", result.guest().fullName());
      assertEquals("guest@example.com", result.guest().email());
      assertEquals("0987654321", result.guest().phone());
      assertEquals(2_000_000L, result.roomSubtotalAmount());
      assertEquals(0L, result.surchargeAmount());
      assertEquals(2_000_000L, result.subtotalAmount());
      assertEquals(2_000_000L, result.totalAmount());
      assertEquals("PENDING_PAYMENT", result.status());
      assertEquals(Instant.parse("2026-11-09T02:15:00Z"), result.expiresAt());
      assertNotNull(result.paymentHandoff());
      assertEquals(2_000_000L, result.paymentHandoff().payableAmount());
      assertEquals("VND", result.paymentHandoff().currency());
      org.mockito.Mockito.verify(inventory)
          .hold(
              org.mockito.ArgumentMatchers.argThat(
                  booking ->
                      booking.id().equals(result.id())
                          && booking.items().get(0).nightlyPrices().size() == 2
                          && booking.totalAmount() == 2_000_000L));
    }

    @Test
    @DisplayName(
        "Từ chối tạo đơn khi tổng báo giá khách xác nhận lệch với giá tính toán lại (PR-05)")
    void shouldRejectWhenQuotedTotalAmountDiffersFromRecalculatedTotal() {
      // Given
      given(accounts.findById(CUSTOMER_ID))
          .willReturn(
              Optional.of(
                  new AccountSnapshot(
                      CUSTOMER_ID,
                      "Nguyễn Văn Đặt",
                      "booker@example.com",
                      true,
                      true,
                      Set.of("CUSTOMER"))));
      given(pricing.calculateQuote(any())).willReturn(sampleQuote());

      CreateBookingCommand command =
          new CreateBookingCommand(
              CUSTOMER_ID,
              HOTEL_ID,
              LocalDate.of(2026, 11, 10),
              LocalDate.of(2026, 11, 12),
              LocalTime.of(8, 0),
              LocalTime.of(12, 0),
              new ContactInput("Nguyễn Văn Đặt", "booker@example.com", "0912345678"),
              null,
              List.of(new BookingItemInput(ROOM_TYPE_ID, 1, 2, 1)),
              2_500_000L);

      // When
      BookingException error =
          assertThrows(BookingException.class, () -> createBookingService.execute(command));

      // Then
      assertEquals(BookingErrorCode.PRICE_CHANGED, error.code());
      org.mockito.Mockito.verifyNoInteractions(bookings, inventory);
    }

    @Test
    @DisplayName("Từ chối tạo đơn khi tài khoản chưa xác minh email (ID-10)")
    void shouldRejectWhenCustomerEmailNotVerified() {
      // Given
      given(accounts.findById(CUSTOMER_ID))
          .willReturn(
              Optional.of(
                  new AccountSnapshot(
                      CUSTOMER_ID,
                      "Nguyễn Văn Đặt",
                      "booker@example.com",
                      true,
                      false,
                      Set.of("CUSTOMER"))));

      CreateBookingCommand command =
          new CreateBookingCommand(
              CUSTOMER_ID,
              HOTEL_ID,
              LocalDate.of(2026, 11, 10),
              LocalDate.of(2026, 11, 12),
              null,
              null,
              new ContactInput("Nguyễn Văn Đặt", "booker@example.com", "0912345678"),
              null,
              List.of(new BookingItemInput(ROOM_TYPE_ID, 1, 2, 0)),
              null);

      // When
      BookingException error =
          assertThrows(BookingException.class, () -> createBookingService.execute(command));

      // Then
      assertEquals(BookingErrorCode.EMAIL_NOT_VERIFIED, error.code());
    }

    @Test
    @DisplayName(
        "Gửi lại cùng Idempotency-Key với payload khớp trả về cùng đơn và chỉ giữ tồn một lần (BK-12)")
    void shouldReturnSameBookingAndReserveOnceWhenReplayedWithSameIdempotencyKey() {
      // Given
      String key = "550e8400-e29b-41d4-a716-446655440000";
      given(accounts.findById(CUSTOMER_ID))
          .willReturn(
              Optional.of(
                  new AccountSnapshot(
                      CUSTOMER_ID,
                      "Nguyễn Văn Đặt",
                      "booker@example.com",
                      true,
                      true,
                      Set.of("CUSTOMER"))));
      given(pricing.calculateQuote(any())).willReturn(sampleQuote());

      AtomicReference<Booking> stored = new AtomicReference<>();
      given(bookings.findByCustomerIdAndIdempotencyKey(CUSTOMER_ID, key))
          .willAnswer(invocation -> Optional.ofNullable(stored.get()));
      given(bookings.add(any(Booking.class)))
          .willAnswer(
              invocation -> {
                Booking b = invocation.getArgument(0);
                stored.set(b);
                return b;
              });

      CreateBookingCommand command =
          new CreateBookingCommand(
              CUSTOMER_ID,
              HOTEL_ID,
              LocalDate.of(2026, 11, 10),
              LocalDate.of(2026, 11, 12),
              LocalTime.of(8, 0),
              LocalTime.of(12, 0),
              new ContactInput("Nguyễn Văn Đặt", "booker@example.com", "0912345678"),
              new ContactInput("Trần Thị Ở", "guest@example.com", "0987654321"),
              List.of(new BookingItemInput(ROOM_TYPE_ID, 1, 2, 1)),
              2_000_000L,
              key);

      // When
      BookingDetailResult first = createBookingService.execute(command);
      BookingDetailResult second = createBookingService.execute(command);

      // Then
      assertEquals(first.id(), second.id());
      assertEquals(first.bookingCode(), second.bookingCode());
      assertEquals(first.totalAmount(), second.totalAmount());
      assertEquals(key, stored.get().idempotencyKey());
      org.mockito.Mockito.verify(bookings, org.mockito.Mockito.times(1)).add(any(Booking.class));
      org.mockito.Mockito.verify(inventory, org.mockito.Mockito.times(1)).hold(any(Booking.class));
      org.mockito.Mockito.verify(pricing, org.mockito.Mockito.times(1)).calculateQuote(any());
    }

    @Test
    @DisplayName(
        "Gửi lại cùng Idempotency-Key với nhiều loại phòng đảo thứ tự và giờ mặc định null vẫn khớp")
    void shouldMatchIdempotentReplayWhenItemsReorderedAndExpectedTimesNull() {
      // Given
      String key = "replay-multi-item_01";
      UUID secondRoomTypeId = UUID.fromString("40000000-0000-0000-0000-000000000099");
      given(accounts.findById(CUSTOMER_ID))
          .willReturn(
              Optional.of(
                  new AccountSnapshot(
                      CUSTOMER_ID,
                      "Nguyễn Văn Đặt",
                      "booker@example.com",
                      true,
                      true,
                      Set.of("CUSTOMER"))));
      CalculatedStayQuote multiQuote =
          new CalculatedStayQuote(
              HOTEL_ID,
              LocalDate.of(2026, 11, 10),
              LocalDate.of(2026, 11, 12),
              3,
              2,
              LocalTime.of(14, 0),
              LocalTime.of(12, 0),
              LocalTime.of(14, 0),
              LocalTime.of(12, 0),
              "VND",
              4_000_000L,
              0L,
              0L,
              0L,
              0L,
              4_000_000L,
              0L,
              4_000_000L,
              List.of(
                  new CalculatedItemQuote(
                      ROOM_TYPE_ID,
                      "Deluxe King",
                      1,
                      2,
                      0,
                      0,
                      2_000_000L,
                      0L,
                      0L,
                      0L,
                      0L,
                      2_000_000L,
                      List.of(
                          new CalculatedNightlyPrice(LocalDate.of(2026, 11, 10), 1_000_000L),
                          new CalculatedNightlyPrice(LocalDate.of(2026, 11, 11), 1_000_000L))),
                  new CalculatedItemQuote(
                      secondRoomTypeId,
                      "Suite",
                      1,
                      2,
                      1,
                      0,
                      2_000_000L,
                      0L,
                      0L,
                      0L,
                      0L,
                      2_000_000L,
                      List.of(
                          new CalculatedNightlyPrice(LocalDate.of(2026, 11, 10), 1_000_000L),
                          new CalculatedNightlyPrice(LocalDate.of(2026, 11, 11), 1_000_000L)))));
      given(pricing.calculateQuote(any())).willReturn(multiQuote);

      AtomicReference<Booking> stored = new AtomicReference<>();
      given(bookings.findByCustomerIdAndIdempotencyKey(CUSTOMER_ID, key))
          .willAnswer(invocation -> Optional.ofNullable(stored.get()));
      given(bookings.add(any(Booking.class)))
          .willAnswer(
              invocation -> {
                Booking b = invocation.getArgument(0);
                stored.set(b);
                return b;
              });

      CreateBookingCommand firstCommand =
          new CreateBookingCommand(
              CUSTOMER_ID,
              HOTEL_ID,
              LocalDate.of(2026, 11, 10),
              LocalDate.of(2026, 11, 12),
              null,
              null,
              new ContactInput("Nguyễn Văn Đặt", "booker@example.com", "0912345678"),
              null,
              List.of(
                  new BookingItemInput(ROOM_TYPE_ID, 1, 2, 0),
                  new BookingItemInput(secondRoomTypeId, 1, 2, 1)),
              4_000_000L,
              key);
      CreateBookingCommand reorderedRetry =
          new CreateBookingCommand(
              CUSTOMER_ID,
              HOTEL_ID,
              LocalDate.of(2026, 11, 10),
              LocalDate.of(2026, 11, 12),
              null,
              null,
              new ContactInput("Nguyễn Văn Đặt", "booker@example.com", "0912345678"),
              null,
              List.of(
                  new BookingItemInput(secondRoomTypeId, 1, 2, 1),
                  new BookingItemInput(ROOM_TYPE_ID, 1, 2, 0)),
              4_000_000L,
              key);

      // When
      BookingDetailResult first = createBookingService.execute(firstCommand);
      BookingDetailResult replay = createBookingService.execute(reorderedRetry);

      // Then
      assertEquals(first.id(), replay.id());
      org.mockito.Mockito.verify(inventory, org.mockito.Mockito.times(1)).hold(any(Booking.class));
    }

    @Test
    @DisplayName(
        "Trả về IDEMPOTENCY_CONFLICT khi dùng lại Idempotency-Key với nội dung đặt phòng khác")
    void shouldThrowConflictWhenReplayedWithMismatchedPayload() {
      // Given
      String key = "550e8400-e29b-41d4-a716-446655440001";
      given(accounts.findById(CUSTOMER_ID))
          .willReturn(
              Optional.of(
                  new AccountSnapshot(
                      CUSTOMER_ID,
                      "Nguyễn Văn Đặt",
                      "booker@example.com",
                      true,
                      true,
                      Set.of("CUSTOMER"))));
      given(pricing.calculateQuote(any())).willReturn(sampleQuote());

      AtomicReference<Booking> stored = new AtomicReference<>();
      given(bookings.findByCustomerIdAndIdempotencyKey(CUSTOMER_ID, key))
          .willAnswer(invocation -> Optional.ofNullable(stored.get()));
      given(bookings.add(any(Booking.class)))
          .willAnswer(
              invocation -> {
                Booking b = invocation.getArgument(0);
                stored.set(b);
                return b;
              });

      CreateBookingCommand original =
          new CreateBookingCommand(
              CUSTOMER_ID,
              HOTEL_ID,
              LocalDate.of(2026, 11, 10),
              LocalDate.of(2026, 11, 12),
              LocalTime.of(8, 0),
              LocalTime.of(12, 0),
              new ContactInput("Nguyễn Văn Đặt", "booker@example.com", "0912345678"),
              new ContactInput("Trần Thị Ở", "guest@example.com", "0987654321"),
              List.of(new BookingItemInput(ROOM_TYPE_ID, 1, 2, 1)),
              2_000_000L,
              key);

      createBookingService.execute(original);

      CreateBookingCommand mismatchedDates =
          new CreateBookingCommand(
              CUSTOMER_ID,
              HOTEL_ID,
              LocalDate.of(2026, 11, 11),
              LocalDate.of(2026, 11, 13),
              LocalTime.of(8, 0),
              LocalTime.of(12, 0),
              new ContactInput("Nguyễn Văn Đặt", "booker@example.com", "0912345678"),
              new ContactInput("Trần Thị Ở", "guest@example.com", "0987654321"),
              List.of(new BookingItemInput(ROOM_TYPE_ID, 1, 2, 1)),
              2_000_000L,
              key);

      // When & Then
      BookingException error =
          assertThrows(BookingException.class, () -> createBookingService.execute(mismatchedDates));
      assertEquals(BookingErrorCode.IDEMPOTENCY_CONFLICT, error.code());
      org.mockito.Mockito.verify(inventory, org.mockito.Mockito.times(1)).hold(any(Booking.class));
    }

    @Test
    @DisplayName(
        "Xử lý race condition khi hai giao dịch cùng chèn Idempotency-Key: giao dịch thua rollback, không giữ tồn và trả đơn của giao dịch thắng")
    void shouldRecoverCleanlyOnConcurrentUniqueConstraintRace() {
      // Given
      String key = "550e8400-e29b-41d4-a716-446655440002";
      given(accounts.findById(CUSTOMER_ID))
          .willReturn(
              Optional.of(
                  new AccountSnapshot(
                      CUSTOMER_ID,
                      "Nguyễn Văn Đặt",
                      "booker@example.com",
                      true,
                      true,
                      Set.of("CUSTOMER"))));
      given(pricing.calculateQuote(any())).willReturn(sampleQuote());

      Booking winningBooking =
          Booking.createPendingPayment(
              UUID.fromString("99999999-0000-0000-0000-000000000009"),
              "BK-20261109-WINNER",
              CUSTOMER_ID,
              HOTEL_ID,
              new Booking.ContactSnapshot("Nguyễn Văn Đặt", "booker@example.com", "0912345678"),
              new Booking.ContactSnapshot("Trần Thị Ở", "guest@example.com", "0987654321"),
              LocalDate.of(2026, 11, 10),
              LocalDate.of(2026, 11, 12),
              3,
              2,
              LocalTime.of(14, 0),
              LocalTime.of(12, 0),
              LocalTime.of(8, 0),
              LocalTime.of(12, 0),
              2,
              1,
              "VND",
              2_000_000L,
              0L,
              0L,
              0L,
              0L,
              2_000_000L,
              0L,
              2_000_000L,
              Instant.parse("2026-11-09T02:15:00Z"),
              Instant.parse("2026-11-09T02:00:00Z"),
              List.of(
                  new Booking.BookingItem(
                      UUID.randomUUID(),
                      ROOM_TYPE_ID,
                      "Deluxe King",
                      1,
                      2,
                      1,
                      0,
                      2_000_000L,
                      0L,
                      0L,
                      0L,
                      0L,
                      2_000_000L,
                      List.of(
                          new Booking.BookingNightPrice(LocalDate.of(2026, 11, 10), 1_000_000L),
                          new Booking.BookingNightPrice(LocalDate.of(2026, 11, 11), 1_000_000L)))),
              key);

      // First lookup sees nothing (race window); insert hits unique constraint; post-rollback
      // REQUIRES_NEW lookup finds the committed winning booking.
      given(bookings.findByCustomerIdAndIdempotencyKey(CUSTOMER_ID, key))
          .willReturn(Optional.empty())
          .willReturn(Optional.of(winningBooking));
      given(bookings.add(any(Booking.class)))
          .willThrow(new DataIntegrityViolationException("uq_bookings_customer_idempotency_key"));

      List<TransactionDefinition> startedTransactions = new java.util.ArrayList<>();
      List<Boolean> rolledBackFlags = new java.util.ArrayList<>();
      PlatformTransactionManager txManager =
          new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
              startedTransactions.add(definition);
              return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
              rolledBackFlags.add(false);
            }

            @Override
            public void rollback(TransactionStatus status) {
              rolledBackFlags.add(true);
            }
          };

      CreateBookingUseCase useCase =
          new BookingConfig()
              .createBookingUseCase(
                  bookings, accounts, pricing, inventory, clock, "PT15M", txManager);

      CreateBookingCommand command =
          new CreateBookingCommand(
              CUSTOMER_ID,
              HOTEL_ID,
              LocalDate.of(2026, 11, 10),
              LocalDate.of(2026, 11, 12),
              LocalTime.of(8, 0),
              LocalTime.of(12, 0),
              new ContactInput("Nguyễn Văn Đặt", "booker@example.com", "0912345678"),
              new ContactInput("Trần Thị Ở", "guest@example.com", "0987654321"),
              List.of(new BookingItemInput(ROOM_TYPE_ID, 1, 2, 1)),
              2_000_000L,
              key);

      // When
      BookingDetailResult recovered = useCase.execute(command);

      // Then
      assertEquals(winningBooking.id(), recovered.id());
      assertEquals(winningBooking.bookingCode(), recovered.bookingCode());
      org.mockito.Mockito.verifyNoInteractions(inventory);
      assertEquals(2, startedTransactions.size());
      assertEquals(List.of(true, false), rolledBackFlags);
      assertEquals(
          TransactionDefinition.PROPAGATION_REQUIRES_NEW,
          startedTransactions.get(1).getPropagationBehavior());
    }
  }

  @Nested
  @DisplayName("Truy vấn danh sách và chi tiết đơn đặt phòng")
  class QueryBookingTests {

    @Test
    @DisplayName("Khách hàng xem được danh sách và chi tiết đơn của chính mình")
    void shouldReturnCustomerBookingsAndDetailWhenOwnedByCustomer() {
      // Given
      given(accounts.findById(CUSTOMER_ID))
          .willReturn(
              Optional.of(
                  new AccountSnapshot(
                      CUSTOMER_ID,
                      "Nguyễn Văn Đặt",
                      "booker@example.com",
                      true,
                      true,
                      Set.of("CUSTOMER"))));
      given(pricing.calculateQuote(any())).willReturn(sampleQuote());
      given(bookings.add(any(Booking.class))).willAnswer(invocation -> invocation.getArgument(0));

      BookingDetailResult created =
          createBookingService.execute(
              new CreateBookingCommand(
                  CUSTOMER_ID,
                  HOTEL_ID,
                  LocalDate.of(2026, 11, 10),
                  LocalDate.of(2026, 11, 12),
                  LocalTime.of(8, 0),
                  LocalTime.of(12, 0),
                  new ContactInput("Nguyễn Văn Đặt", "booker@example.com", "0912345678"),
                  new ContactInput("Trần Thị Ở", "guest@example.com", "0987654321"),
                  List.of(new BookingItemInput(ROOM_TYPE_ID, 1, 2, 1)),
                  2_000_000L));

      Booking domainBooking =
          Booking.rehydrate(
              created.id(),
              created.bookingCode(),
              CUSTOMER_ID,
              HOTEL_ID,
              new Booking.ContactSnapshot(
                  created.booker().fullName(), created.booker().email(), created.booker().phone()),
              new Booking.ContactSnapshot(
                  created.guest().fullName(), created.guest().email(), created.guest().phone()),
              created.checkIn(),
              created.checkOut(),
              created.days(),
              created.nights(),
              created.standardCheckInTime(),
              created.standardCheckOutTime(),
              created.expectedCheckInTime(),
              created.expectedCheckOutTime(),
              created.adults(),
              created.children(),
              Booking.Status.PENDING_PAYMENT,
              created.currency(),
              created.roomSubtotalAmount(),
              created.earlyCheckInSurchargeAmount(),
              created.lateCheckOutSurchargeAmount(),
              created.extraGuestSurchargeAmount(),
              created.surchargeAmount(),
              created.subtotalAmount(),
              created.discountAmount(),
              created.totalAmount(),
              created.expiresAt(),
              created.createdAt(),
              created.updatedAt(),
              0L,
              List.of());

      PageQuery pagination = new PageQuery(0, 10);
      given(bookings.findByCustomerId(CUSTOMER_ID, null, pagination))
          .willReturn(new PageResult<>(List.of(domainBooking), 0, 10, 1L, 1, true, true));
      given(bookings.findByIdAndCustomerId(created.id(), CUSTOMER_ID))
          .willReturn(Optional.of(domainBooking));

      // When
      PageResult<BookingSummaryResult> page =
          listCustomerBookingsService.execute(
              new ListCustomerBookingsQuery(CUSTOMER_ID, null, pagination));
      BookingDetailResult detail =
          getCustomerBookingDetailService.execute(
              new GetCustomerBookingDetailQuery(CUSTOMER_ID, created.id()));

      // Then
      assertEquals(1, page.content().size());
      assertEquals(created.bookingCode(), page.content().get(0).bookingCode());
      assertEquals(created.id(), detail.id());
      assertEquals("Trần Thị Ở", detail.guest().fullName());
    }

    @Test
    @DisplayName("Quản lý khách sạn xem được danh sách và chi tiết đơn thuộc khách sạn sở hữu")
    void shouldReturnManagementBookingsWhenActorOwnsHotel() {
      // Given
      given(accounts.findById(OWNER_ID))
          .willReturn(
              Optional.of(
                  new AccountSnapshot(
                      OWNER_ID,
                      "Chủ Khách Sạn",
                      "owner@example.com",
                      true,
                      true,
                      Set.of("HOTEL_OWNER"))));
      given(hotels.findById(HOTEL_ID))
          .willReturn(Optional.of(new HotelSnapshot(HOTEL_ID, OWNER_ID, "Khách sạn A", true)));

      UUID bookingId = UUID.randomUUID();
      Booking domainBooking =
          Booking.rehydrate(
              bookingId,
              "BK-20261109-ABCD",
              CUSTOMER_ID,
              HOTEL_ID,
              new Booking.ContactSnapshot("Nguyễn Văn Đặt", "booker@example.com", "0912345678"),
              new Booking.ContactSnapshot("Trần Thị Ở", "guest@example.com", "0987654321"),
              LocalDate.of(2026, 11, 10),
              LocalDate.of(2026, 11, 12),
              3,
              2,
              LocalTime.of(14, 0),
              LocalTime.of(12, 0),
              LocalTime.of(14, 0),
              LocalTime.of(12, 0),
              2,
              0,
              Booking.Status.PENDING_PAYMENT,
              "VND",
              2_000_000L,
              0L,
              0L,
              0L,
              0L,
              2_000_000L,
              0L,
              2_000_000L,
              Instant.parse("2026-11-09T02:15:00Z"),
              Instant.parse("2026-11-09T02:00:00Z"),
              Instant.parse("2026-11-09T02:00:00Z"),
              0L,
              List.of());

      PageQuery pagination = new PageQuery(0, 10);
      given(bookings.findByHotelId(HOTEL_ID, null, pagination))
          .willReturn(new PageResult<>(List.of(domainBooking), 0, 10, 1L, 1, true, true));
      given(bookings.findByIdAndHotelId(bookingId, HOTEL_ID))
          .willReturn(Optional.of(domainBooking));

      // When
      PageResult<BookingSummaryResult> page =
          listManagementBookingsService.execute(
              new ListManagementBookingsQuery(OWNER_ID, HOTEL_ID, null, pagination));
      BookingDetailResult detail =
          getManagementBookingDetailService.execute(
              new GetManagementBookingDetailQuery(OWNER_ID, HOTEL_ID, bookingId));

      // Then
      assertEquals(1, page.content().size());
      assertEquals(bookingId, detail.id());
    }
  }
}
