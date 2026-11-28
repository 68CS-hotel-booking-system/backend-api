package com.team.hotelbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.team.hotelbooking.booking.application.command.CreateBookingCommand;
import com.team.hotelbooking.booking.application.command.CreateBookingCommand.BookingItemInput;
import com.team.hotelbooking.booking.application.command.CreateBookingCommand.ContactInput;
import com.team.hotelbooking.booking.application.usecase.CreateBookingUseCase;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Hủy đơn và hoàn tiền an toàn")
class BookingCancellationIntegrationTests {
  private static final Instant NOW = Instant.parse("2027-01-01T00:00:00Z");
  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;
  @Autowired CreateBookingUseCase create;
  @Autowired com.team.hotelbooking.booking.BookingCheckoutDirectory checkout;
  @MockitoBean Clock clock;
  UUID owner, admin, customer, hotel, roomType;

  @BeforeEach
  void setUp() {
    given(clock.instant()).willReturn(NOW);
    given(clock.getZone()).willReturn(ZoneOffset.UTC);
    given(clock.withZone(org.mockito.ArgumentMatchers.any()))
        .willAnswer(invocation -> Clock.fixed(clock.instant(), invocation.getArgument(0)));
    owner = user("HOTEL_OWNER");
    admin = user("ADMIN");
    customer = user("CUSTOMER");
    hotel = UUID.randomUUID();
    roomType = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO hotels(id, owner_user_id, city_id, name, slug, address, status)
        VALUES (?, ?, '822cafba-73f6-4ec0-bae7-71804535cae3', 'Commission test', ?, 'Da Nang', 'ACTIVE')
        """,
        hotel,
        owner,
        "commission-" + hotel);
    jdbc.update(
        """
        INSERT INTO room_types(id, hotel_id, name, max_occupancy, max_adults, max_children, base_price, is_active)
        VALUES (?, ?, 'Commission room', 2, 2, 0, 2000005, TRUE)
        """,
        roomType,
        hotel);
    jdbc.update(
        """
        INSERT INTO rooms(id, hotel_id, room_type_id, room_number, readiness, operational_from)
        VALUES (?, ?, ?, 'C01', 'READY', '2026-01-01T00:00:00Z')
        """,
        UUID.randomUUID(),
        hotel,
        roomType);
    contract(NOW.minusSeconds(60), NOW.plusSeconds(3600), "ACTIVE", "10");
  }

  @Test
  void shouldCancelReleaseInventoryAndReplayWithoutDuplicatingHistory() throws Exception {
    UUID id = booking();
    cancel(id, customer)
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.data.status").value("CANCELLED"))
        .andExpect(jsonPath("$.data.cancellation.refundStatus").value("NOT_REQUIRED"))
        .andExpect(jsonPath("$.data.cancellation.refundAmount").value(0));
    cancel(id, customer)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    assertThat(
            jdbc.queryForObject("SELECT status::text FROM bookings WHERE id = ?", String.class, id))
        .isEqualTo("CANCELLED");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM inventory_reservations WHERE booking_id = ? AND status = 'HELD'",
                Long.class,
                id))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM booking_status_history WHERE booking_id = ? AND to_status = 'CANCELLED'",
                Long.class,
                id))
        .isEqualTo(1);
    assertThat(checkout.confirmPayment(id, NOW)).isFalse();
  }

  @Test
  void shouldHideOtherCustomersBookingAndRequireAuthentication() throws Exception {
    UUID id = booking();
    cancel(id, owner).andExpect(status().isNotFound());
    mvc.perform(post("/api/v1/bookings/" + id + "/cancel")).andExpect(status().isUnauthorized());
    assertThat(
            jdbc.queryForObject("SELECT status::text FROM bookings WHERE id = ?", String.class, id))
        .isEqualTo("PENDING_PAYMENT");
  }

  @Test
  void shouldExpireAtExactDeadlineAndReleaseHold() throws Exception {
    UUID id = booking();
    given(clock.instant()).willReturn(NOW.plusSeconds(900));
    cancel(id, customer)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("EXPIRED"));
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM inventory_reservations WHERE booking_id = ? AND status = 'RELEASED'",
                Long.class,
                id))
        .isEqualTo(1);
  }

  @Test
  void shouldRejectPaidStatesAndActivePaymentAttemptsWithoutReleasingInventory() throws Exception {
    UUID id = booking();
    for (String state : List.of("CONFIRMED", "CHECKED_IN", "CHECKED_OUT", "NO_SHOW")) {
      jdbc.update("UPDATE bookings SET status = ?::booking_status__t WHERE id = ?", state, id);
      cancel(id, customer)
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("BOOKING_NOT_CANCELLABLE"));
    }
    jdbc.update("UPDATE bookings SET status = 'PENDING_PAYMENT' WHERE id = ?", id);
    jdbc.update(
        """
        INSERT INTO payment_transactions(id,booking_id,customer_id,amount_vnd,status,payment_url,created_at,expires_at)
        VALUES (?,?,?,2000005,'PENDING','https://example.test/pay',?,?)
        """,
        UUID.randomUUID(),
        id,
        customer,
        java.sql.Timestamp.from(NOW),
        java.sql.Timestamp.from(NOW.plusSeconds(900)));
    cancel(id, customer)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("BOOKING_PAYMENT_IN_PROGRESS"));
    jdbc.update("UPDATE payment_transactions SET status = 'SUCCEEDED' WHERE booking_id = ?", id);
    cancel(id, customer).andExpect(status().isConflict());
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM inventory_reservations WHERE booking_id = ? AND status = 'HELD'",
                Long.class,
                id))
        .isEqualTo(1);
  }

  @Test
  void shouldSerializeTwoConcurrentCancellationRequests() throws Exception {
    UUID id = booking();
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<Integer> call =
          () -> cancel(id, customer).andReturn().getResponse().getStatus();
      var results = executor.invokeAll(List.of(call, call));
      assertThat(results.get(0).get()).isEqualTo(200);
      assertThat(results.get(1).get()).isEqualTo(200);
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM booking_status_history WHERE booking_id = ? AND to_status = 'CANCELLED'",
                Long.class,
                id))
        .isEqualTo(1);
  }

  @Test
  void shouldRollBackInventoryReleaseIfStatusWriteFails() throws Exception {
    UUID id = booking();
    jdbc.execute(
        """
        CREATE OR REPLACE FUNCTION reject_test_cancel() RETURNS trigger LANGUAGE plpgsql AS $$
        BEGIN IF NEW.status = 'CANCELLED' THEN RAISE EXCEPTION 'simulated cancel failure'; END IF; RETURN NEW; END; $$
        """);
    jdbc.execute(
        "CREATE TRIGGER test_cancel_fail BEFORE UPDATE ON bookings FOR EACH ROW EXECUTE FUNCTION reject_test_cancel()");
    try {
      cancel(id, customer).andExpect(status().is5xxServerError());
      assertThat(
              jdbc.queryForObject(
                  "SELECT status::text FROM bookings WHERE id = ?", String.class, id))
          .isEqualTo("PENDING_PAYMENT");
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM inventory_reservations WHERE booking_id = ? AND status = 'HELD'",
                  Long.class,
                  id))
          .isEqualTo(1);
    } finally {
      jdbc.execute("DROP TRIGGER test_cancel_fail ON bookings");
      jdbc.execute("DROP FUNCTION reject_test_cancel()");
    }
  }

  @Test
  void shouldNeverReleaseInventoryOfAConcurrentSuccessfulConfirmation() throws Exception {
    UUID id = booking();
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var confirmation = executor.submit(() -> checkout.confirmPayment(id, NOW));
      var cancellation =
          executor.submit(() -> cancel(id, customer).andReturn().getResponse().getStatus());
      if (confirmation.get()) {
        assertThat(cancellation.get()).isEqualTo(409);
        assertThat(
                jdbc.queryForObject(
                    "SELECT count(*) FROM inventory_reservations WHERE booking_id = ? AND status = 'CONFIRMED'",
                    Long.class,
                    id))
            .isEqualTo(1);
      } else {
        assertThat(cancellation.get()).isEqualTo(200);
        assertThat(
                jdbc.queryForObject(
                    "SELECT status::text FROM bookings WHERE id = ?", String.class, id))
            .isEqualTo("CANCELLED");
      }
    }
  }

  @Test
  void paidCancellationShouldQuoteRefundReleaseAssignedRoomAndPreserveCommission()
      throws Exception {
    UUID id = paidBooking();
    UUID item =
        jdbc.queryForObject("SELECT id FROM booking_items WHERE booking_id=?", UUID.class, id);
    UUID room = jdbc.queryForObject("SELECT id FROM rooms WHERE hotel_id=?", UUID.class, hotel);
    UUID assignment = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO room_assignments(id,hotel_id,booking_id,booking_item_id,room_type_id,room_id,room_number_snapshot,start_date,end_date,status,assigned_by_user_id,assigned_at) VALUES (?,?,?,?,?,?,'C01','2027-01-10','2027-01-11','ASSIGNED',?,?)",
        assignment,
        hotel,
        id,
        item,
        roomType,
        room,
        owner,
        java.sql.Timestamp.from(NOW));
    var snapshot = jdbc.queryForMap("SELECT * FROM booking_commissions WHERE booking_id=?", id);
    mvc.perform(get("/api/v1/bookings/" + id + "/cancellation").with(actor(customer, "CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.canCancel").value(true))
        .andExpect(jsonPath("$.data.refundAmount").value(2000005));
    paidCancel(id)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.cancellation.refundStatus").value("PENDING"))
        .andExpect(jsonPath("$.data.cancellation.refundAmount").value(2000005));
    cancel(id, customer)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.cancellation.refundAmount").value(2000005));
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM payment_refunds WHERE booking_id=?", Long.class, id))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM inventory_reservations WHERE booking_id=? AND status='CONFIRMED'",
                Long.class,
                id))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT status::text FROM room_assignments WHERE id=?", String.class, assignment))
        .isEqualTo("RELEASED");
    assertThat(
            jdbc.queryForObject(
                "SELECT released_by_user_id FROM room_assignments WHERE id=?",
                UUID.class,
                assignment))
        .isEqualTo(customer);
    assertThat(jdbc.queryForMap("SELECT * FROM booking_commissions WHERE booking_id=?", id))
        .isEqualTo(snapshot);
    mvc.perform(get("/api/v1/finance/dashboard").with(actor(owner, "HOTEL_OWNER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.platformAmount").value(0))
        .andExpect(jsonPath("$.data.ownerAmount").value(0))
        .andExpect(jsonPath("$.data.pendingRefundAmount").value(2000005))
        .andExpect(jsonPath("$.data.netCollectedAmount").value(2000005));
  }

  @Test
  void cancellationCutoffMustUseVietnamTimeAndRejectCheckInAndStaleQuote() throws Exception {
    UUID id = paidBooking();
    given(clock.instant()).willReturn(Instant.parse("2027-01-10T07:00:00Z"));
    paidCancel(id).andExpect(status().isConflict());
    mvc.perform(get("/api/v1/bookings/" + id + "/cancellation").with(actor(customer, "CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.canCancel").value(false));
    given(clock.instant()).willReturn(NOW);
    mvc.perform(
            post("/api/v1/bookings/" + id + "/cancel")
                .with(actor(customer, "CUSTOMER"))
                .contentType("application/json")
                .content("{\"expectedVersion\":999,\"expectedRefundAmount\":2000005}"))
        .andExpect(status().isConflict());
    jdbc.update("UPDATE bookings SET status='CHECKED_IN' WHERE id=?", id);
    paidCancel(id).andExpect(status().isConflict());
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM payment_refunds WHERE booking_id=?", Long.class, id))
        .isZero();
  }

  @Test
  void manualRefundRequiresActualAdminAndReferenceAndRecordsHistoryAndNetCash() throws Exception {
    UUID id = paidBooking();
    paidCancel(id).andExpect(status().isOk());
    UUID refund = refund(id);
    process(refund, owner, "HOTEL_OWNER", "PROCESSING", 0, null).andExpect(status().isForbidden());
    process(refund, customer, "ADMIN", "PROCESSING", 0, null).andExpect(status().isForbidden());
    process(refund, admin, "ADMIN", "COMPLETED", 0, "abc").andExpect(status().isConflict());
    process(refund, admin, "ADMIN", "PROCESSING", 0, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.version").value(1));
    process(refund, admin, "ADMIN", "COMPLETED", 1, null).andExpect(status().isBadRequest());
    String reference = "REF-" + UUID.randomUUID();
    process(refund, admin, "ADMIN", "COMPLETED", 1, reference)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.history.length()").value(2));
    process(refund, admin, "ADMIN", "COMPLETED", 1, reference).andExpect(status().isConflict());
    mvc.perform(get("/api/v1/me/refunds/" + refund).with(actor(customer, "CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.canProcess").value(false));
    mvc.perform(get("/api/v1/finance/dashboard").with(actor(owner, "HOTEL_OWNER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.pendingRefundAmount").value(0))
        .andExpect(jsonPath("$.data.refundedAmount").value(2000005))
        .andExpect(jsonPath("$.data.netCollectedAmount").value(0));
  }

  @Test
  void refundReadScopeMustHideOtherCustomersAndOtherOwnersEvenOnInactiveHotel() throws Exception {
    UUID id = paidBooking();
    paidCancel(id).andExpect(status().isOk());
    UUID refund = refund(id);
    UUID stranger = user("HOTEL_OWNER");
    mvc.perform(get("/api/v1/me/refunds/" + refund).with(actor(stranger, "CUSTOMER")))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/management/refunds/" + refund).with(actor(stranger, "HOTEL_OWNER")))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/management/refunds").with(actor(stranger, "HOTEL_OWNER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(0));
    mvc.perform(get("/api/v1/management/refunds").with(actor(customer, "HOTEL_OWNER")))
        .andExpect(status().isForbidden());
    jdbc.update("UPDATE hotels SET status='SUSPENDED' WHERE id=?", hotel);
    mvc.perform(get("/api/v1/management/refunds/" + refund).with(actor(owner, "HOTEL_OWNER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.canProcess").value(false));
    mvc.perform(
            get("/api/v1/me/refunds")
                .param("bookingId", id.toString())
                .with(actor(customer, "CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));
    mvc.perform(get("/api/v1/bookings/" + id + "/cancellation").with(actor(stranger, "CUSTOMER")))
        .andExpect(status().isNotFound());
  }

  @Test
  void twoPaidCancellationsShouldCreateOnlyOneRefundAndHistory() throws Exception {
    UUID id = paidBooking();
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<Integer> call =
          () -> paidCancel(id).andReturn().getResponse().getStatus();
      for (var result : executor.invokeAll(List.of(call, call)))
        assertThat(result.get()).isEqualTo(200);
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM payment_refunds WHERE booking_id=?", Long.class, id))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM booking_status_history WHERE booking_id=? AND to_status='CANCELLED'",
                Long.class,
                id))
        .isEqualTo(1);
  }

  @Test
  void processingMustBeAtomicAndRejectCompetingAdmins() throws Exception {
    UUID id = paidBooking();
    paidCancel(id).andExpect(status().isOk());
    UUID refund = refund(id);
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<Integer> call =
          () ->
              process(refund, admin, "ADMIN", "PROCESSING", 0, null)
                  .andReturn()
                  .getResponse()
                  .getStatus();
      var values = executor.invokeAll(List.of(call, call));
      assertThat(List.of(values.get(0).get(), values.get(1).get()))
          .containsExactlyInAnyOrder(200, 409);
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM payment_refund_actions WHERE refund_id=?",
                Long.class,
                refund))
        .isEqualTo(1);
  }

  @Test
  void paidCancellationMustRollbackRefundAndInventoryWhenBookingWriteFails() throws Exception {
    UUID id = paidBooking();
    jdbc.execute(
        "CREATE OR REPLACE FUNCTION reject_paid_cancel() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.id = '"
            + id
            + "' AND NEW.status='CANCELLED' THEN RAISE EXCEPTION 'test failure'; END IF; RETURN NEW; END; $$");
    jdbc.execute(
        "CREATE TRIGGER paid_cancel_fail BEFORE UPDATE ON bookings FOR EACH ROW EXECUTE FUNCTION reject_paid_cancel()");
    try {
      paidCancel(id).andExpect(status().is5xxServerError());
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM payment_refunds WHERE booking_id=?", Long.class, id))
          .isZero();
      assertThat(
              jdbc.queryForObject("SELECT status::text FROM bookings WHERE id=?", String.class, id))
          .isEqualTo("CONFIRMED");
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM inventory_reservations WHERE booking_id=? AND status='CONFIRMED'",
                  Long.class,
                  id))
          .isEqualTo(1);
    } finally {
      jdbc.execute("DROP TRIGGER paid_cancel_fail ON bookings");
      jdbc.execute("DROP FUNCTION reject_paid_cancel()");
    }
  }

  @Test
  void settlementReferenceMustBeUniqueIgnoringCase() throws Exception {
    UUID first = paidBooking();
    paidCancel(first).andExpect(status().isOk());
    UUID firstRefund = refund(first);
    UUID second = paidBooking();
    paidCancel(second).andExpect(status().isOk());
    UUID secondRefund = refund(second);
    String reference = "UNIQUE-" + UUID.randomUUID();
    process(firstRefund, admin, "ADMIN", "PROCESSING", 0, null).andExpect(status().isOk());
    process(firstRefund, admin, "ADMIN", "COMPLETED", 1, reference).andExpect(status().isOk());
    process(secondRefund, admin, "ADMIN", "PROCESSING", 0, null).andExpect(status().isOk());
    process(secondRefund, admin, "ADMIN", "COMPLETED", 1, reference.toLowerCase())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("REFUND_REFERENCE_CONFLICT"));
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM payment_refunds WHERE id=?", String.class, secondRefund))
        .isEqualTo("PROCESSING");
  }

  private UUID paidBooking() {
    UUID id = booking();
    assertThat(checkout.confirmPayment(id, NOW)).isTrue();
    jdbc.update(
        "INSERT INTO payment_transactions(id,booking_id,customer_id,amount_vnd,status,payment_url,created_at,expires_at,booking_confirmed) VALUES (?,?,?,2000005,'SUCCEEDED','https://example.test/pay',?,?,true)",
        UUID.randomUUID(),
        id,
        customer,
        java.sql.Timestamp.from(NOW),
        java.sql.Timestamp.from(NOW.plusSeconds(900)));
    return id;
  }

  private UUID refund(UUID id) {
    return jdbc.queryForObject("SELECT id FROM payment_refunds WHERE booking_id=?", UUID.class, id);
  }

  private org.springframework.test.web.servlet.ResultActions paidCancel(UUID id) throws Exception {
    long version = jdbc.queryForObject("SELECT version FROM bookings WHERE id=?", Long.class, id);
    return mvc.perform(
        post("/api/v1/bookings/" + id + "/cancel")
            .with(actor(customer, "CUSTOMER"))
            .contentType("application/json")
            .content("{\"expectedVersion\":" + version + ",\"expectedRefundAmount\":2000005}"));
  }

  private org.springframework.test.web.servlet.ResultActions process(
      UUID id, UUID who, String role, String target, long version, String reference)
      throws Exception {
    return mvc.perform(
        post("/api/v1/admin/refunds/" + id + "/actions")
            .with(actor(who, role))
            .contentType("application/json")
            .content(
                "{\"status\":\""
                    + target
                    + "\",\"expectedVersion\":"
                    + version
                    + ",\"note\":\"Thông báo cho khách\""
                    + (reference == null ? "" : ",\"reference\":\"" + reference + "\"")
                    + "}"));
  }

  private org.springframework.test.web.servlet.ResultActions cancel(UUID id, UUID actorId)
      throws Exception {
    return mvc.perform(post("/api/v1/bookings/" + id + "/cancel").with(actor(actorId, "CUSTOMER")));
  }

  private UUID booking() {
    return create
        .execute(
            new CreateBookingCommand(
                customer,
                hotel,
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 1, 11),
                LocalTime.of(14, 0),
                LocalTime.of(12, 0),
                new ContactInput("Khách test", "guest@example.test", "+84980008881"),
                null,
                List.of(new BookingItemInput(roomType, 1, 2, 0)),
                2000005L))
        .id();
  }

  private UUID user(String role) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO users(id,email,password_hash,full_name,phone,status,email_verified_at) VALUES (?,?,'unused','Test',?,'active',now())",
        id,
        id + "@commission.test",
        "+" + (100000000000L + Math.abs(id.hashCode())));
    jdbc.update(
        "INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE role_key = ?",
        id,
        role);
    return id;
  }

  private UUID contract(Instant start, Instant end, String state, String rate) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO hotel_contracts(id,contract_code,hotel_id,owner_user_id,commission_rate,commission_basis,starts_at,ends_at,status,created_by_user_id)
        VALUES (?,?,?,?,?,'AFTER_DISCOUNT',?,?,?::hotel_contract_status__t,?)
        """,
        id,
        "CT-" + id,
        hotel,
        owner,
        new BigDecimal(rate),
        java.sql.Timestamp.from(start),
        java.sql.Timestamp.from(end),
        state,
        admin);
    return id;
  }

  private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
          .JwtRequestPostProcessor
      actor(UUID id, String role) {
    return jwt()
        .jwt(token -> token.subject(id.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }
}
