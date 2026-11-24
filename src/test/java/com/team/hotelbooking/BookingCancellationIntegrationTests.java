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
@DisplayName("Hủy đơn chưa thanh toán an toàn")
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
