package com.team.hotelbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.hotelbooking.booking.application.command.CreateBookingCommand;
import com.team.hotelbooking.booking.application.command.CreateBookingCommand.BookingItemInput;
import com.team.hotelbooking.booking.application.command.CreateBookingCommand.ContactInput;
import com.team.hotelbooking.booking.application.usecase.CreateBookingUseCase;
import com.team.hotelbooking.inventory.InventoryReservationDirectory;
import com.team.hotelbooking.inventory.InventoryReservationDirectory.HoldItemRequest;
import com.team.hotelbooking.inventory.InventoryReservationDirectory.HoldRequest;
import com.team.hotelbooking.inventory.InventoryReservationDirectory.ReleaseOutcome;
import com.team.hotelbooking.inventory.InventoryReservationDirectory.ReleaseReason;
import com.team.hotelbooking.inventory.application.command.AssignRoomsCommand;
import com.team.hotelbooking.inventory.application.command.CheckInBookingCommand;
import com.team.hotelbooking.inventory.application.command.CheckOutBookingCommand;
import com.team.hotelbooking.inventory.application.command.CreateRoomBlockCommand;
import com.team.hotelbooking.inventory.application.command.MarkNoShowCommand;
import com.team.hotelbooking.inventory.application.command.UpdateRoomReadinessCommand;
import com.team.hotelbooking.inventory.application.result.BookingStayOperationResult;
import com.team.hotelbooking.inventory.application.result.RoomReadinessResult;
import com.team.hotelbooking.inventory.application.usecase.AssignRoomsUseCase;
import com.team.hotelbooking.inventory.application.usecase.CheckInBookingUseCase;
import com.team.hotelbooking.inventory.application.usecase.CheckOutBookingUseCase;
import com.team.hotelbooking.inventory.application.usecase.CreateRoomBlockUseCase;
import com.team.hotelbooking.inventory.application.usecase.MarkNoShowUseCase;
import com.team.hotelbooking.inventory.application.usecase.UpdateRoomReadinessUseCase;
import com.team.hotelbooking.inventory.domain.exception.InventoryErrorCode;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName(
    "Kiểm thử tích hợp tồn kho, phân bổ liên tục và chống tranh phòng cuối trên PostgreSQL")
class InventoryConcurrencyIntegrationTests {

  private static PostgreSQLContainer postgresContainer;

  @DynamicPropertySource
  static void configureDataSource(DynamicPropertyRegistry registry) {
    String externalUrl = System.getProperty("test.postgres.url");
    if (externalUrl != null && !externalUrl.isBlank()) {
      registry.add("spring.datasource.url", () -> externalUrl);
      registry.add(
          "spring.datasource.username",
          () -> System.getProperty("test.postgres.username", "postgres"));
      registry.add(
          "spring.datasource.password", () -> System.getProperty("test.postgres.password", ""));
    } else {
      postgresContainer = new PostgreSQLContainer(DockerImageName.parse("postgres:17"));
      postgresContainer.start();
      registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
      registry.add("spring.datasource.username", postgresContainer::getUsername);
      registry.add("spring.datasource.password", postgresContainer::getPassword);
    }
  }

  private static final UUID OWNER_ID = UUID.fromString("11111111-2222-4333-8444-555555555555");
  private static final UUID HOTEL_ID = UUID.fromString("22222222-3333-4444-8555-666666666666");
  private static final UUID DA_NANG_ID = UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3");
  private static final UUID SINGLE_ROOM_TYPE_ID =
      UUID.fromString("33333333-4444-4555-8666-777777777777");
  private static final UUID TWO_ROOM_TYPE_ID =
      UUID.fromString("44444444-5555-4666-8777-888888888888");
  private static final UUID ROOM_101_ID = UUID.fromString("55555555-6666-4777-8888-999999999991");
  private static final UUID ROOM_301_ID = UUID.fromString("55555555-6666-4777-8888-999999999992");
  private static final UUID ROOM_302_ID = UUID.fromString("55555555-6666-4777-8888-999999999993");

  @Autowired private InventoryReservationDirectory inventoryReservationDirectory;
  @Autowired private AssignRoomsUseCase assignRoomsUseCase;
  @Autowired private CreateRoomBlockUseCase createRoomBlockUseCase;
  @Autowired private CheckInBookingUseCase checkInBookingUseCase;
  @Autowired private CheckOutBookingUseCase checkOutBookingUseCase;
  @Autowired private MarkNoShowUseCase markNoShowUseCase;
  @Autowired private UpdateRoomReadinessUseCase updateRoomReadinessUseCase;
  @Autowired private MockMvc mvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private CreateBookingUseCase createBookingUseCase;

  @BeforeEach
  void setUp() {
    jdbc.update("DELETE FROM room_assignments WHERE hotel_id = ?", HOTEL_ID);
    jdbc.update("DELETE FROM booking_stay_operations WHERE hotel_id = ?", HOTEL_ID);
    jdbc.update(
        "DELETE FROM inventory_reservations WHERE room_type_id IN (?, ?)",
        SINGLE_ROOM_TYPE_ID,
        TWO_ROOM_TYPE_ID);
    jdbc.update("DELETE FROM room_blocks WHERE hotel_id = ?", HOTEL_ID);
    jdbc.update("DELETE FROM bookings WHERE hotel_id = ?", HOTEL_ID);

    jdbc.update(
        """
        INSERT INTO users (id, email, password_hash, full_name, phone, status)
        VALUES (?, 'inventory-owner@test.local', 'hash', 'Chủ khách sạn tồn kho', '+84909998877', 'active'::user_status__t)
        ON CONFLICT (id) DO NOTHING
        """,
        OWNER_ID);

    jdbc.update(
        """
        INSERT INTO hotels (id, owner_user_id, city_id, name, slug, address, star_rating, status)
        VALUES (?, ?, ?, 'Khách sạn Kiểm Thử Tồn Kho', 'khach-san-kiem-thu-ton-kho', '123 Võ Nguyên Giáp', 4, 'ACTIVE'::hotel_status__t)
        ON CONFLICT (id) DO NOTHING
        """,
        HOTEL_ID,
        OWNER_ID,
        DA_NANG_ID);

    jdbc.update(
        """
        INSERT INTO room_types (id, hotel_id, name, max_occupancy, max_adults, max_children, base_price, is_active)
        VALUES (?, ?, 'Superior Single', 2, 2, 0, 1200000, TRUE),
               (?, ?, 'Deluxe Twin', 2, 2, 0, 1500000, TRUE)
        ON CONFLICT (id) DO NOTHING
        """,
        SINGLE_ROOM_TYPE_ID,
        HOTEL_ID,
        TWO_ROOM_TYPE_ID,
        HOTEL_ID);

    jdbc.update(
        """
        INSERT INTO rooms (id, hotel_id, room_type_id, room_number, floor, readiness, operational_from)
        VALUES (?, ?, ?, '101', '1', 'READY'::room_readiness__t, '2026-01-01T00:00:00Z'::timestamptz),
               (?, ?, ?, '301', '3', 'READY'::room_readiness__t, '2026-01-01T00:00:00Z'::timestamptz),
               (?, ?, ?, '302', '3', 'READY'::room_readiness__t, '2026-01-01T00:00:00Z'::timestamptz)
        ON CONFLICT (id) DO NOTHING
        """,
        ROOM_101_ID,
        HOTEL_ID,
        SINGLE_ROOM_TYPE_ID,
        ROOM_301_ID,
        HOTEL_ID,
        TWO_ROOM_TYPE_ID,
        ROOM_302_ID,
        HOTEL_ID,
        TWO_ROOM_TYPE_ID);
    jdbc.update("UPDATE hotels SET status = 'ACTIVE'::hotel_status__t WHERE id = ?", HOTEL_ID);
    jdbc.update("UPDATE room_types SET is_active = TRUE WHERE hotel_id = ?", HOTEL_ID);
    jdbc.update(
        "UPDATE rooms SET readiness = 'READY'::room_readiness__t WHERE hotel_id = ?", HOTEL_ID);
  }

  private CreateBookingCommand bookingRequest(LocalDate checkIn, UUID roomTypeId, long amount) {
    return new CreateBookingCommand(
        OWNER_ID,
        HOTEL_ID,
        checkIn,
        checkIn.plusDays(2),
        java.time.LocalTime.of(4, 0),
        java.time.LocalTime.of(23, 0),
        new ContactInput("Khách test", "guest@test.local", "+84909998877"),
        null,
        List.of(new BookingItemInput(roomTypeId, 1, 2, 0)),
        amount);
  }

  @Test
  @DisplayName("Tạo đơn chốt giá theo đêm và giữ phòng; đơn kế tiếp hết phòng phải rollback")
  void shouldSaveNightlyPricesAndRollbackSecondBookingWhenLastRoomAlreadyHeld() {
    jdbc.update("UPDATE users SET email_verified_at = now() WHERE id = ?", OWNER_ID);
    LocalDate checkIn = LocalDate.now().plusDays(10);
    var command = bookingRequest(checkIn, SINGLE_ROOM_TYPE_ID, 2_400_000L);

    var result = createBookingUseCase.execute(command);

    assertThat(result.totalAmount()).isEqualTo(2_400_000L);
    assertThat(result.surchargeAmount()).isZero();
    assertThat(result.items().getFirst().nightlyPrices()).hasSize(2);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM inventory_reservations WHERE booking_id = ? AND status = 'HELD'",
                Integer.class,
                result.id()))
        .isEqualTo(2);

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> createBookingUseCase.execute(command))
        .isInstanceOf(InventoryException.class)
        .extracting(ex -> ((InventoryException) ex).code())
        .isEqualTo(InventoryErrorCode.INSUFFICIENT_INVENTORY);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE hotel_id = ?", Integer.class, HOTEL_ID))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM booking_items WHERE booking_id = ?",
                Integer.class,
                result.id()))
        .isEqualTo(1);
  }

  @Test
  @DisplayName(
      "20 luồng đồng thời tranh giữ 1 phòng vật lý cuối cùng: đúng 1 luồng thành công (IV-08)")
  void shouldAllowExactlyOneWinnerWhenTwentyThreadsRaceForLastRoom() throws Exception {
    int threadCount = 20;
    LocalDate checkIn = LocalDate.now().plusDays(10);
    LocalDate checkOut = checkIn.plusDays(2);
    Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

    ExecutorService pool = Executors.newFixedThreadPool(threadCount);
    CountDownLatch readyLatch = new CountDownLatch(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger insufficientCount = new AtomicInteger(0);

    List<Callable<Void>> tasks = new ArrayList<>(threadCount);
    for (int i = 0; i < threadCount; i++) {
      tasks.add(
          () -> {
            readyLatch.countDown();
            startLatch.await(10, TimeUnit.SECONDS);
            HoldRequest request =
                new HoldRequest(
                    UUID.randomUUID(),
                    HOTEL_ID,
                    checkIn,
                    checkOut,
                    expiresAt,
                    List.of(new HoldItemRequest(UUID.randomUUID(), SINGLE_ROOM_TYPE_ID, 1)));
            try {
              inventoryReservationDirectory.hold(request);
              successCount.incrementAndGet();
            } catch (InventoryException ex) {
              if (ex.code() == InventoryErrorCode.INSUFFICIENT_INVENTORY) {
                insufficientCount.incrementAndGet();
              } else {
                throw ex;
              }
            }
            return null;
          });
    }

    List<Future<Void>> futures = new ArrayList<>(threadCount);
    for (Callable<Void> task : tasks) {
      futures.add(pool.submit(task));
    }
    assertThat(readyLatch.await(10, TimeUnit.SECONDS)).isTrue();
    startLatch.countDown();

    for (Future<Void> future : futures) {
      future.get(20, TimeUnit.SECONDS);
    }
    pool.shutdownNow();

    assertThat(successCount.get()).isEqualTo(1);
    assertThat(insufficientCount.get()).isEqualTo(threadCount - 1);
  }

  @Test
  @DisplayName(
      "Phòng 301 chặn đêm 1, phòng 302 chặn đêm 2 => IV01 báo không còn phòng trống liên tục 2 đêm (IV-13)")
  void shouldReportZeroAvailabilityWhenSplitRoomBlocksPreventContinuousStay() throws Exception {
    LocalDate night1 = LocalDate.now().plusDays(20);
    LocalDate night2 = night1.plusDays(1);
    LocalDate checkOut = night1.plusDays(2);

    jdbc.update(
        """
        INSERT INTO room_blocks (id, hotel_id, room_type_id, room_id, start_date, end_date, reason, status, created_by_user_id)
        VALUES (?, ?, ?, ?, ?, ?, 'Bảo trì 301 đêm 1', 'ACTIVE'::room_block_status__t, ?),
               (?, ?, ?, ?, ?, ?, 'Bảo trì 302 đêm 2', 'ACTIVE'::room_block_status__t, ?)
        """,
        UUID.randomUUID(),
        HOTEL_ID,
        TWO_ROOM_TYPE_ID,
        ROOM_301_ID,
        night1,
        night1,
        OWNER_ID,
        UUID.randomUUID(),
        HOTEL_ID,
        TWO_ROOM_TYPE_ID,
        ROOM_302_ID,
        night2,
        night2,
        OWNER_ID);

    mvc.perform(
            get("/api/v1/hotels/{hotelId}/availability", HOTEL_ID)
                .param("checkInDate", night1.toString())
                .param("checkOutDate", checkOut.toString())
                .param("rooms", "1")
                .param("adults", "2")
                .param("children", "0"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                    "$.data.roomTypes[?(@.roomTypeId == '%s')].availableQuantity",
                    TWO_ROOM_TYPE_ID.toString())
                .value(0))
        .andExpect(
            jsonPath(
                    "$.data.roomTypes[?(@.roomTypeId == '%s')].isAvailable",
                    TWO_ROOM_TYPE_ID.toString())
                .value(false));

    jdbc.update("UPDATE users SET email_verified_at = now() WHERE id = ?", OWNER_ID);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                createBookingUseCase.execute(bookingRequest(night1, TWO_ROOM_TYPE_ID, 3_000_000L)))
        .isInstanceOf(InventoryException.class)
        .extracting(ex -> ((InventoryException) ex).code())
        .isEqualTo(InventoryErrorCode.INSUFFICIENT_INVENTORY);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE hotel_id = ?", Integer.class, HOTEL_ID))
        .isZero();
  }

  @Test
  @DisplayName("Gọi release song song nhiều luồng chỉ hoàn trả tồn kho đúng một lần (IV-07)")
  void shouldReleaseInventoryAtMostOnceUnderConcurrentReleaseCalls() throws Exception {
    UUID bookingId = UUID.randomUUID();
    LocalDate checkIn = LocalDate.now().plusDays(30);
    LocalDate checkOut = checkIn.plusDays(3);

    inventoryReservationDirectory.hold(
        new HoldRequest(
            bookingId,
            HOTEL_ID,
            checkIn,
            checkOut,
            Instant.now().plus(15, ChronoUnit.MINUTES),
            List.of(new HoldItemRequest(UUID.randomUUID(), SINGLE_ROOM_TYPE_ID, 1))));

    int threads = 10;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch startLatch = new CountDownLatch(1);
    AtomicInteger totalReleasedRows = new AtomicInteger(0);
    AtomicInteger nonZeroCallCount = new AtomicInteger(0);

    List<Future<Void>> futures = new ArrayList<>(threads);
    for (int i = 0; i < threads; i++) {
      futures.add(
          pool.submit(
              () -> {
                startLatch.await(5, TimeUnit.SECONDS);
                ReleaseOutcome outcome =
                    inventoryReservationDirectory.release(bookingId, ReleaseReason.CANCELLED);
                if (outcome.releasedRowCount() > 0) {
                  nonZeroCallCount.incrementAndGet();
                  totalReleasedRows.addAndGet(outcome.releasedRowCount());
                }
                return null;
              }));
    }
    startLatch.countDown();
    for (Future<Void> f : futures) {
      f.get(10, TimeUnit.SECONDS);
    }
    pool.shutdownNow();

    assertThat(nonZeroCallCount.get()).isEqualTo(1);
    assertThat(totalReleasedRows.get()).isEqualTo(3);
  }

  @Test
  @DisplayName(
      "Giữ chỗ (hold) song song với tạo lịch chặn phòng (block) trên 1 phòng duy nhất: chỉ đúng 1 thao tác thành công (IV-08, BL-06)")
  void shouldAllowEitherHoldOrBlockWhenRacingForSingleRoom() throws Exception {
    LocalDate checkIn = LocalDate.now().plusDays(40);
    LocalDate checkOut = checkIn.plusDays(2);
    Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch readyLatch = new CountDownLatch(2);
    CountDownLatch startLatch = new CountDownLatch(1);
    AtomicInteger holdWins = new AtomicInteger(0);
    AtomicInteger blockWins = new AtomicInteger(0);

    Future<Void> holdFuture =
        pool.submit(
            () -> {
              readyLatch.countDown();
              startLatch.await(5, TimeUnit.SECONDS);
              try {
                inventoryReservationDirectory.hold(
                    new HoldRequest(
                        UUID.randomUUID(),
                        HOTEL_ID,
                        checkIn,
                        checkOut,
                        expiresAt,
                        List.of(new HoldItemRequest(UUID.randomUUID(), SINGLE_ROOM_TYPE_ID, 1))));
                holdWins.incrementAndGet();
              } catch (InventoryException ex) {
                assertThat(ex.code()).isEqualTo(InventoryErrorCode.INSUFFICIENT_INVENTORY);
              }
              return null;
            });

    Future<Void> blockFuture =
        pool.submit(
            () -> {
              readyLatch.countDown();
              startLatch.await(5, TimeUnit.SECONDS);
              try {
                createRoomBlockUseCase.execute(
                    new CreateRoomBlockCommand(
                        OWNER_ID,
                        HOTEL_ID,
                        ROOM_101_ID,
                        checkIn,
                        checkOut.minusDays(1),
                        "Bảo trì đột xuất phòng 101"));
                blockWins.incrementAndGet();
              } catch (InventoryException ex) {
                assertThat(ex.code())
                    .isEqualTo(InventoryErrorCode.ROOM_BLOCK_CONFLICTS_RESERVATION);
              }
              return null;
            });

    assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
    startLatch.countDown();
    holdFuture.get(10, TimeUnit.SECONDS);
    blockFuture.get(10, TimeUnit.SECONDS);
    pool.shutdownNow();

    assertThat(holdWins.get() + blockWins.get()).isEqualTo(1);
  }

  @Test
  @DisplayName(
      "Hai lễ tân đồng thời gán cùng phòng 301 cho 2 đơn CONFIRMED chồng lịch: chỉ đúng 1 đơn gán thành công (BK-15, BK-16)")
  void shouldAllowOnlyOneReceptionistToAssignSamePhysicalRoomToOverlappingBookings()
      throws Exception {
    LocalDate checkIn = LocalDate.now().plusDays(50);
    LocalDate checkOut = checkIn.plusDays(2);
    Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

    UUID bookingA = UUID.randomUUID();
    UUID itemA = UUID.randomUUID();
    UUID bookingB = UUID.randomUUID();
    UUID itemB = UUID.randomUUID();

    inventoryReservationDirectory.hold(
        new HoldRequest(
            bookingA,
            HOTEL_ID,
            checkIn,
            checkOut,
            expiresAt,
            List.of(new HoldItemRequest(itemA, TWO_ROOM_TYPE_ID, 1))));
    inventoryReservationDirectory.confirm(bookingA);

    inventoryReservationDirectory.hold(
        new HoldRequest(
            bookingB,
            HOTEL_ID,
            checkIn,
            checkOut,
            expiresAt,
            List.of(new HoldItemRequest(itemB, TWO_ROOM_TYPE_ID, 1))));
    inventoryReservationDirectory.confirm(bookingB);

    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch readyLatch = new CountDownLatch(2);
    CountDownLatch startLatch = new CountDownLatch(1);
    AtomicInteger assignedSuccess = new AtomicInteger(0);
    AtomicInteger conflictFailures = new AtomicInteger(0);

    Callable<Void> taskA =
        () -> {
          readyLatch.countDown();
          startLatch.await(5, TimeUnit.SECONDS);
          try {
            assignRoomsUseCase.execute(
                new AssignRoomsCommand(
                    OWNER_ID,
                    HOTEL_ID,
                    bookingA,
                    List.of(new AssignRoomsCommand.RoomAssignmentItem(itemA, List.of(ROOM_301_ID))),
                    "Lễ tân A gán phòng 301"));
            assignedSuccess.incrementAndGet();
          } catch (InventoryException ex) {
            if (ex.code() == InventoryErrorCode.ROOM_ASSIGNMENT_CONFLICT) {
              conflictFailures.incrementAndGet();
            } else {
              throw ex;
            }
          }
          return null;
        };

    Callable<Void> taskB =
        () -> {
          readyLatch.countDown();
          startLatch.await(5, TimeUnit.SECONDS);
          try {
            assignRoomsUseCase.execute(
                new AssignRoomsCommand(
                    OWNER_ID,
                    HOTEL_ID,
                    bookingB,
                    List.of(new AssignRoomsCommand.RoomAssignmentItem(itemB, List.of(ROOM_301_ID))),
                    "Lễ tân B gán phòng 301"));
            assignedSuccess.incrementAndGet();
          } catch (InventoryException ex) {
            if (ex.code() == InventoryErrorCode.ROOM_ASSIGNMENT_CONFLICT) {
              conflictFailures.incrementAndGet();
            } else {
              throw ex;
            }
          }
          return null;
        };

    Future<Void> fA = pool.submit(taskA);
    Future<Void> fB = pool.submit(taskB);
    assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
    startLatch.countDown();
    fA.get(10, TimeUnit.SECONDS);
    fB.get(10, TimeUnit.SECONDS);
    pool.shutdownNow();

    assertThat(assignedSuccess.get()).isEqualTo(1);
    assertThat(conflictFailures.get()).isEqualTo(1);
  }

  @Test
  @DisplayName(
      "Kịch bản vận hành lưu trú: Phòng DIRTY chặn check-in -> Đánh dấu READY -> Check-in -> Check-out tự chuyển DIRTY (BK-08, BK-18, IV-10)")
  void shouldEnforceRoomReadinessAcrossCheckInAndCheckOutLifecycle() {
    LocalDate checkIn = LocalDate.now();
    LocalDate checkOut = checkIn.plusDays(2);
    UUID bookingId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    insertConfirmedBooking(bookingId, checkIn, checkOut);

    inventoryReservationDirectory.hold(
        new HoldRequest(
            bookingId,
            HOTEL_ID,
            checkIn,
            checkOut,
            Instant.now().plus(15, ChronoUnit.MINUTES),
            List.of(new HoldItemRequest(itemId, SINGLE_ROOM_TYPE_ID, 1))));
    inventoryReservationDirectory.confirm(bookingId);

    assignRoomsUseCase.execute(
        new AssignRoomsCommand(
            OWNER_ID,
            HOTEL_ID,
            bookingId,
            List.of(new AssignRoomsCommand.RoomAssignmentItem(itemId, List.of(ROOM_101_ID))),
            "Phân phòng 101 trước khi nhận phòng"));

    updateRoomReadinessUseCase.execute(
        new UpdateRoomReadinessCommand(OWNER_ID, HOTEL_ID, ROOM_101_ID, "DIRTY"));

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                checkInBookingUseCase.execute(
                    new CheckInBookingCommand(OWNER_ID, HOTEL_ID, bookingId)))
        .isInstanceOf(InventoryException.class)
        .extracting(ex -> ((InventoryException) ex).code())
        .isEqualTo(InventoryErrorCode.ROOM_NOT_READY);

    RoomReadinessResult readyResult =
        updateRoomReadinessUseCase.execute(
            new UpdateRoomReadinessCommand(OWNER_ID, HOTEL_ID, ROOM_101_ID, "READY"));
    assertThat(readyResult.readiness()).isEqualTo("READY");

    BookingStayOperationResult checkedIn =
        checkInBookingUseCase.execute(new CheckInBookingCommand(OWNER_ID, HOTEL_ID, bookingId));
    assertThat(checkedIn.stayStatus()).isEqualTo("CHECKED_IN");
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                assignRoomsUseCase.execute(
                    new AssignRoomsCommand(
                        OWNER_ID,
                        HOTEL_ID,
                        bookingId,
                        List.of(
                            new AssignRoomsCommand.RoomAssignmentItem(
                                itemId, List.of(ROOM_101_ID))),
                        "Đổi phòng sau khi nhận")))
        .isInstanceOf(InventoryException.class)
        .extracting(ex -> ((InventoryException) ex).code())
        .isEqualTo(InventoryErrorCode.INVALID_STAY_OPERATION);
    assertThat(
            jdbc.queryForObject(
                "SELECT status::text FROM bookings WHERE id = ?", String.class, bookingId))
        .isEqualTo("CHECKED_IN");

    jdbc.update("UPDATE hotels SET status = 'DRAFT'::hotel_status__t WHERE id = ?", HOTEL_ID);
    jdbc.update("UPDATE room_types SET is_active = FALSE WHERE id = ?", SINGLE_ROOM_TYPE_ID);

    BookingStayOperationResult checkedOut =
        checkOutBookingUseCase.execute(new CheckOutBookingCommand(OWNER_ID, HOTEL_ID, bookingId));
    assertThat(checkedOut.stayStatus()).isEqualTo("CHECKED_OUT");
    assertThat(
            jdbc.queryForObject(
                "SELECT status::text FROM bookings WHERE id = ?", String.class, bookingId))
        .isEqualTo("CHECKED_OUT");

    String readinessInDb =
        jdbc.queryForObject(
            "SELECT readiness::text FROM rooms WHERE id = ?", String.class, ROOM_101_ID);
    assertThat(readinessInDb).isEqualTo("DIRTY");
  }

  @Test
  @DisplayName(
      "Kịch bản No-show: Đơn quá hạn nhận phòng -> đánh dấu NO_SHOW chỉ hoàn trả đêm tương lai và gọi lặp không hoàn trả lần hai (BK-10, IV-11)")
  void shouldReleaseOnlyFutureNightsOnceWhenMarkedNoShow() {
    LocalDate yesterday = LocalDate.now().minusDays(1);
    LocalDate today = LocalDate.now();
    LocalDate tomorrow = LocalDate.now().plusDays(1);
    UUID bookingId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    insertConfirmedBooking(bookingId, yesterday, tomorrow.plusDays(1));

    jdbc.update(
        """
        INSERT INTO inventory_reservations (id, booking_id, booking_item_id, room_type_id, stay_date, quantity, status)
        VALUES (?, ?, ?, ?, ?, 1, 'CONFIRMED'::inventory_reservation_status__t),
               (?, ?, ?, ?, ?, 1, 'CONFIRMED'::inventory_reservation_status__t),
               (?, ?, ?, ?, ?, 1, 'CONFIRMED'::inventory_reservation_status__t)
        """,
        UUID.randomUUID(),
        bookingId,
        itemId,
        SINGLE_ROOM_TYPE_ID,
        yesterday,
        UUID.randomUUID(),
        bookingId,
        itemId,
        SINGLE_ROOM_TYPE_ID,
        today,
        UUID.randomUUID(),
        bookingId,
        itemId,
        SINGLE_ROOM_TYPE_ID,
        tomorrow);

    BookingStayOperationResult firstNoShow =
        markNoShowUseCase.execute(
            new MarkNoShowCommand(OWNER_ID, HOTEL_ID, bookingId, "Khách không đến nhận phòng"));
    assertThat(firstNoShow.stayStatus()).isEqualTo("NO_SHOW");
    assertThat(firstNoShow.releasedRoomNights()).isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "SELECT status::text FROM bookings WHERE id = ?", String.class, bookingId))
        .isEqualTo("NO_SHOW");

    BookingStayOperationResult secondNoShow =
        markNoShowUseCase.execute(
            new MarkNoShowCommand(OWNER_ID, HOTEL_ID, bookingId, "Bấm lặp lần 2"));
    assertThat(secondNoShow.stayStatus()).isEqualTo("NO_SHOW");
    assertThat(secondNoShow.releasedRoomNights()).isEqualTo(0);
  }

  @Test
  @DisplayName("Từ chối sửa lịch chặn khi người gọi không thuộc khách sạn")
  void shouldRejectRoomBlockUpdateOutsideHotelScope() throws Exception {
    LocalDate start = LocalDate.now().plusDays(10);
    var block =
        createRoomBlockUseCase.execute(
            new CreateRoomBlockCommand(OWNER_ID, HOTEL_ID, ROOM_101_ID, start, start, "Bảo trì"));
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                    "/api/v1/management/hotels/{hotelId}/room-blocks/{blockId}",
                    HOTEL_ID,
                    block.id())
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.jwt()
                        .jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))
                        .authorities(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_HOTEL_OWNER")))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(
                    "{\"startDate\":\""
                        + start
                        + "\",\"endDate\":\""
                        + start.plusDays(1)
                        + "\",\"reason\":\"Sửa trái phép\"}"))
        .andExpect(status().isForbidden());
    assertThat(
            jdbc.queryForObject(
                "SELECT end_date FROM room_blocks WHERE id = ?", LocalDate.class, block.id()))
        .isEqualTo(start);
  }

  @Test
  @DisplayName("Gán đơn ngắn phải bảo vệ đủ kỳ ở của đơn dài và rollback khi hết phương án")
  void shouldProtectLongerUnassignedStay() {
    LocalDate start = LocalDate.now().plusDays(10);
    createRoomBlockUseCase.execute(
        new CreateRoomBlockCommand(
            OWNER_ID,
            HOTEL_ID,
            ROOM_302_ID,
            start.plusDays(1),
            start.plusDays(1),
            "Bảo trì đêm thứ hai"));
    UUID longBooking = UUID.randomUUID();
    UUID shortBooking = UUID.randomUUID();
    UUID shortItem = UUID.randomUUID();
    inventoryReservationDirectory.hold(
        new HoldRequest(
            longBooking,
            HOTEL_ID,
            start,
            start.plusDays(2),
            Instant.now().plusSeconds(900),
            List.of(new HoldItemRequest(UUID.randomUUID(), TWO_ROOM_TYPE_ID, 1))));
    inventoryReservationDirectory.confirm(longBooking);
    inventoryReservationDirectory.hold(
        new HoldRequest(
            shortBooking,
            HOTEL_ID,
            start,
            start.plusDays(1),
            Instant.now().plusSeconds(900),
            List.of(new HoldItemRequest(shortItem, TWO_ROOM_TYPE_ID, 1))));
    inventoryReservationDirectory.confirm(shortBooking);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                assignRoomsUseCase.execute(
                    new AssignRoomsCommand(
                        OWNER_ID,
                        HOTEL_ID,
                        shortBooking,
                        List.of(
                            new AssignRoomsCommand.RoomAssignmentItem(
                                shortItem, List.of(ROOM_301_ID))),
                        null)))
        .isInstanceOf(InventoryException.class)
        .extracting(ex -> ((InventoryException) ex).code())
        .isEqualTo(InventoryErrorCode.ROOM_ASSIGNMENT_CONFLICT);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM room_assignments WHERE booking_id = ?",
                Integer.class,
                shortBooking))
        .isZero();
    assignRoomsUseCase.execute(
        new AssignRoomsCommand(
            OWNER_ID,
            HOTEL_ID,
            shortBooking,
            List.of(new AssignRoomsCommand.RoomAssignmentItem(shortItem, List.of(ROOM_302_ID))),
            null));
  }

  @Test
  @DisplayName("Không tìm thấy booking chính phải rollback toàn bộ thao tác check-in")
  void shouldRollbackWhenBookingStatusCannotBeUpdated() {
    LocalDate today = LocalDate.now();
    UUID bookingId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    inventoryReservationDirectory.hold(
        new HoldRequest(
            bookingId,
            HOTEL_ID,
            today,
            today.plusDays(2),
            Instant.now().plusSeconds(900),
            List.of(new HoldItemRequest(itemId, SINGLE_ROOM_TYPE_ID, 1))));
    inventoryReservationDirectory.confirm(bookingId);
    assignRoomsUseCase.execute(
        new AssignRoomsCommand(
            OWNER_ID,
            HOTEL_ID,
            bookingId,
            List.of(new AssignRoomsCommand.RoomAssignmentItem(itemId, List.of(ROOM_101_ID))),
            null));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                checkInBookingUseCase.execute(
                    new CheckInBookingCommand(OWNER_ID, HOTEL_ID, bookingId)))
        .isInstanceOf(InventoryException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM booking_stay_operations WHERE booking_id = ?",
                Integer.class,
                bookingId))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT status::text FROM room_assignments WHERE booking_id = ?",
                String.class,
                bookingId))
        .isEqualTo("ASSIGNED");
  }

  private void insertConfirmedBooking(UUID bookingId, LocalDate checkIn, LocalDate checkOut) {
    int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    jdbc.update(
        """
        INSERT INTO bookings (id, booking_code, customer_id, hotel_id,
          booker_name_snapshot, booker_email_snapshot, booker_phone_snapshot,
          guest_name_snapshot, guest_email_snapshot, guest_phone_snapshot,
          check_in, check_out, days, nights, status)
        VALUES (?, ?, ?, ?, 'Khách test', 'guest@test.local', '+84909998877',
          'Khách test', 'guest@test.local', '+84909998877', ?, ?, ?, ?, 'CONFIRMED'::booking_status__t)
        """,
        bookingId,
        bookingId.toString().substring(0, 20),
        OWNER_ID,
        HOTEL_ID,
        checkIn,
        checkOut,
        nights + 1,
        nights);
  }
}
