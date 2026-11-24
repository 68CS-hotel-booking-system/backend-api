package com.team.hotelbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.team.hotelbooking.booking.application.command.CreateBookingCommand;
import com.team.hotelbooking.booking.application.command.CreateBookingCommand.BookingItemInput;
import com.team.hotelbooking.booking.application.command.CreateBookingCommand.ContactInput;
import com.team.hotelbooking.booking.application.usecase.CreateBookingUseCase;
import com.team.hotelbooking.booking.domain.exception.BookingErrorCode;
import com.team.hotelbooking.booking.domain.exception.BookingException;
import com.team.hotelbooking.booking.domain.model.CommissionAmounts;
import com.team.hotelbooking.payment.ContractDirectory;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Hợp đồng mở bán, gia hạn và snapshot hoa hồng")
class ContractCommissionIntegrationTests {
  private static final Instant NOW = Instant.parse("2027-01-01T00:00:00Z");
  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;
  @Autowired CreateBookingUseCase create;
  @Autowired com.team.hotelbooking.booking.BookingCheckoutDirectory checkout;
  @Autowired ContractDirectory contracts;
  @MockitoBean Clock clock;
  UUID owner, admin, customer, hotel, roomType, contract;

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
    contract = contract(NOW.minusSeconds(60), NOW.plusSeconds(3600), "ACTIVE", "10");
  }

  @Test
  void shouldHideOnlyHotelsWithoutEffectiveContractAndKeepDirectLinkInformative() throws Exception {
    assertThat(contracts.findEffectiveHotelIds(NOW)).contains(hotel);
    given(clock.instant()).willReturn(NOW.plusSeconds(3600));
    assertThat(contracts.findEffectiveHotelIds(clock.instant())).doesNotContain(hotel);
    mvc.perform(get("/api/v1/hotels").param("keyword", "Commission test"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[?(@.id == '" + hotel + "')]").isEmpty());
    mvc.perform(get("/api/v1/hotels/search").param("keyword", "Commission test"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[?(@.id == '" + hotel + "')]").isEmpty());
    mvc.perform(get("/api/v1/hotels/" + hotel))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.acceptingBookings").value(false));
    var error = assertThrows(BookingException.class, () -> booking());
    assertThat(error.code()).isEqualTo(BookingErrorCode.CONTRACT_NOT_EFFECTIVE);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE hotel_id = ?", Long.class, hotel))
        .isZero();
  }

  @Test
  void shouldStoreCommissionWithoutExposingItToCustomerAndKeepItAfterExpiry() throws Exception {
    UUID booking = booking();
    var snapshot =
        jdbc.queryForMap("SELECT * FROM booking_commissions WHERE booking_id = ?", booking);
    assertThat(((Number) snapshot.get("basis_amount")).longValue()).isEqualTo(2000005);
    assertThat(((Number) snapshot.get("platform_amount")).longValue()).isEqualTo(200001);
    assertThat(((Number) snapshot.get("owner_amount")).longValue()).isEqualTo(1800004);
    assertThat(snapshot.get("contract_id")).isEqualTo(contract);
    // Thay đổi điều khoản sau đó không tác động snapshot đã lưu.
    jdbc.update("UPDATE hotel_contracts SET commission_rate = 20 WHERE id = ?", contract);
    given(clock.instant()).willReturn(NOW.plusSeconds(3600));
    mvc.perform(get("/api/v1/bookings/" + booking).with(actor(customer, "CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.commissionRate").doesNotExist())
        .andExpect(jsonPath("$.data.platformAmount").doesNotExist())
        .andExpect(jsonPath("$.data.totalAmount").value(2000005));
    assertThat(jdbc.queryForMap("SELECT * FROM booking_commissions WHERE booking_id = ?", booking))
        .isEqualTo(snapshot);
  }

  @Test
  void shouldRejectMismatchedOwnerAndNonFixedBasis() throws Exception {
    mvc.perform(
            post("/api/v1/admin/hotels/" + hotel + "/contracts")
                .with(actor(admin, "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(customer, "DRAFT", NOW.plusSeconds(3600), NOW.plusSeconds(7200))))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/admin/hotels/" + hotel + "/contracts")
                .with(actor(admin, "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body(owner, "DRAFT", NOW.plusSeconds(3600), NOW.plusSeconds(7200))
                        .replace("AFTER_DISCOUNT", "BEFORE_DISCOUNT")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRenewWithoutEditingOldTermsAndKeepContinuousSalesAtBoundary() throws Exception {
    var before = jdbc.queryForMap("SELECT * FROM hotel_contracts WHERE id = ?", contract);
    mvc.perform(
            post("/api/v1/admin/hotels/" + hotel + "/contracts/" + contract + "/renewals")
                .with(actor(admin, "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(owner, "ACTIVE", NOW.plusSeconds(3600), NOW.plusSeconds(7200))))
        .andExpect(status().isCreated())
        .andExpect(
            jsonPath("$.data.contractCode")
                .value(org.hamcrest.Matchers.matchesPattern("HD-2027-[0-9]{6,}")));
    assertThat(jdbc.queryForMap("SELECT * FROM hotel_contracts WHERE id = ?", contract))
        .isEqualTo(before);
    var next = contracts.findEffective(hotel, NOW.plusSeconds(3600)).orElseThrow();
    assertThat(next.contractId()).isNotEqualTo(contract);
    assertThat(
            jdbc.queryForObject(
                "SELECT previous_contract_id FROM hotel_contract_renewals WHERE next_contract_id = ?",
                UUID.class,
                next.contractId()))
        .isEqualTo(contract);
    given(clock.instant()).willReturn(NOW.plusSeconds(3600));
    mvc.perform(get("/api/v1/hotels/" + hotel))
        .andExpect(jsonPath("$.data.acceptingBookings").value(true));
  }

  @Test
  void shouldRestrictRenewalAndRejectOverlappingDates() throws Exception {
    String path = "/api/v1/admin/hotels/" + hotel + "/contracts/" + contract + "/renewals";
    String body = body(owner, "ACTIVE", NOW, NOW.plusSeconds(7200));
    mvc.perform(
            post(path)
                .with(actor(owner, "HOTEL_OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden());
    mvc.perform(
            post(path)
                .with(actor(admin, "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReportOnlyCompletedCommissionAndScopeOwnerData() throws Exception {
    UUID booking = booking();
    mvc.perform(get("/api/v1/finance/dashboard").with(actor(owner, "HOTEL_OWNER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.platformAmount").value(0))
        .andExpect(jsonPath("$.data.collectedAmount").value(0));
    jdbc.update("UPDATE bookings SET status = 'CHECKED_OUT' WHERE id = ?", booking);
    jdbc.update(
        """
        INSERT INTO payment_transactions(id, booking_id, customer_id, amount_vnd, currency, status,
          payment_url, created_at, expires_at, booking_confirmed, paid_at)
        VALUES (?, ?, ?, 2000005, 'VND', 'SUCCEEDED', 'https://example.test/pay', now(), now() + interval '15 minutes', true, now())
        """,
        UUID.randomUUID(),
        booking,
        customer);
    mvc.perform(get("/api/v1/finance/dashboard").with(actor(owner, "HOTEL_OWNER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.completedAmount").value(2000005))
        .andExpect(jsonPath("$.data.collectedAmount").value(2000005))
        .andExpect(jsonPath("$.data.platformAmount").value(200001))
        .andExpect(jsonPath("$.data.ownerAmount").value(1800004))
        .andExpect(jsonPath("$.data.hotels.length()").value(1));
    UUID outsider = user("HOTEL_OWNER");
    mvc.perform(get("/api/v1/finance/dashboard").with(actor(outsider, "HOTEL_OWNER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.collectedAmount").value(0))
        .andExpect(jsonPath("$.data.hotels").isEmpty());
    mvc.perform(get("/api/v1/finance/dashboard").with(actor(customer, "CUSTOMER")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/finance/dashboard").with(actor(admin, "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"));
  }

  @Test
  void shouldDeriveExpiredStatusAndLeaveHotelStateActive() throws Exception {
    given(clock.instant()).willReturn(NOW.plusSeconds(3600));
    mvc.perform(
            get("/api/v1/finance/hotels/" + hotel + "/contracts")
                .param("status", "EXPIRED")
                .with(actor(owner, "HOTEL_OWNER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].status").value("EXPIRED"));
    mvc.perform(get("/api/v1/finance/dashboard").with(actor(owner, "HOTEL_OWNER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.hotels[0].contractStatus").value("EXPIRED"))
        .andExpect(jsonPath("$.data.hotels[0].acceptingBookings").value(false));
    assertThat(
            jdbc.queryForObject(
                "SELECT status::text FROM hotels WHERE id = ?", String.class, hotel))
        .isEqualTo("ACTIVE");
  }

  @Test
  void shouldKeepLegacyCompletedRevenueAndFlagMissingCommission() throws Exception {
    UUID booking = booking();
    jdbc.update("DELETE FROM booking_commissions WHERE booking_id = ?", booking);
    jdbc.update("UPDATE bookings SET status = 'CHECKED_OUT' WHERE id = ?", booking);
    mvc.perform(get("/api/v1/finance/dashboard").with(actor(owner, "HOTEL_OWNER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.completedAmount").value(2000005))
        .andExpect(jsonPath("$.data.platformAmount").value(0))
        .andExpect(jsonPath("$.data.missingCommissionCount").value(1));
  }

  @Test
  void shouldKeepExistingBookingPayableWithinHoldAfterContractExpires() {
    jdbc.update(
        "UPDATE hotel_contracts SET ends_at = ? WHERE id = ?",
        java.sql.Timestamp.from(NOW.plusSeconds(60)),
        contract);
    UUID booking = booking();
    given(clock.instant()).willReturn(NOW.plusSeconds(60));
    assertThat(contracts.findEffective(hotel, clock.instant())).isEmpty();
    assertThat(checkout.confirmPayment(booking, clock.instant())).isTrue();
  }

  @Test
  void shouldCalculateDiscountedRoomCommissionAndLeaveSurchargesToOwner() {
    var amounts = CommissionAmounts.calculate(1000000, 100000, 1100000, new BigDecimal("10"));
    assertThat(amounts).isEqualTo(new CommissionAmounts(900000, 90000, 1010000));
  }

  @Test
  void shouldReportTheCapturedRatesSeparatelyAfterRenewal() throws Exception {
    UUID first = booking();
    jdbc.update("UPDATE bookings SET status='CHECKED_OUT' WHERE id=?", first);
    pay(first);
    // Mô phỏng thay đổi dữ liệu hợp đồng sau khi đã chốt: báo cáo phải đọc rate từ snapshot.
    jdbc.update("UPDATE hotel_contracts SET commission_rate=25 WHERE id=?", contract);
    mvc.perform(
            post("/api/v1/admin/hotels/" + hotel + "/contracts/" + contract + "/renewals")
                .with(actor(admin, "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body(owner, "ACTIVE", NOW.plusSeconds(3600), NOW.plusSeconds(7200))
                        .replace("\"commissionRate\":12", "\"commissionRate\":20")))
        .andExpect(status().isCreated());
    given(clock.instant()).willReturn(NOW.plusSeconds(3600));
    UUID second = booking();
    jdbc.update("UPDATE bookings SET status='CHECKED_OUT' WHERE id=?", second);
    pay(second);
    var report = dashboard(owner, "HOTEL_OWNER");
    assertThat(report.path("completedAmount").asLong()).isEqualTo(4000010);
    assertThat(report.path("platformAmount").asLong()).isEqualTo(600002);
    assertThat(report.path("ownerAmount").asLong()).isEqualTo(3400008);
    assertThat(report.path("contracts").size()).isEqualTo(2);
    boolean oldFound = false, newFound = false;
    for (var row : report.path("contracts")) {
      if (row.path("contractId").asText().equals(contract.toString())) {
        oldFound = true;
        assertThat(new BigDecimal(row.path("commissionRate").asText())).isEqualByComparingTo("10");
        assertThat(row.path("platformAmount").asLong()).isEqualTo(200001);
      } else {
        newFound = true;
        assertThat(new BigDecimal(row.path("commissionRate").asText())).isEqualByComparingTo("20");
        assertThat(row.path("platformAmount").asLong()).isEqualTo(400001);
      }
      assertThat(row.path("platformAmount").asLong() + row.path("ownerAmount").asLong())
          .isEqualTo(row.path("completedAmount").asLong());
    }
    assertThat(oldFound && newFound).isTrue();
  }

  @Test
  void shouldRetainRevenueForTheContractOwnerWhenHotelOwnershipChanges() throws Exception {
    UUID first = booking();
    jdbc.update("UPDATE bookings SET status='CHECKED_OUT' WHERE id=?", first);
    pay(first);
    UUID nextOwner = user("HOTEL_OWNER");
    jdbc.update("UPDATE hotel_contracts SET status='TERMINATED' WHERE id=?", contract);
    jdbc.update("UPDATE hotels SET owner_user_id=? WHERE id=?", nextOwner, hotel);
    mvc.perform(
            post("/api/v1/admin/hotels/" + hotel + "/contracts")
                .with(actor(admin, "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(nextOwner, "ACTIVE", NOW.plusSeconds(900), NOW.plusSeconds(7200))))
        .andExpect(status().isCreated());
    given(clock.instant()).willReturn(NOW.plusSeconds(901));
    var before = dashboard(nextOwner, "HOTEL_OWNER");
    assertThat(before.path("platformAmount").asLong()).isZero();
    assertThat(before.path("ownerAmount").asLong()).isZero();
    assertThat(before.path("collectedAmount").asLong()).isZero();
    UUID second = booking();
    jdbc.update("UPDATE bookings SET status='CHECKED_OUT' WHERE id=?", second);
    pay(second);
    var previous = dashboard(owner, "HOTEL_OWNER");
    assertThat(previous.path("ownerAmount").asLong()).isEqualTo(1800004);
    assertThat(previous.path("collectedAmount").asLong()).isEqualTo(2000005);
    assertThat(previous.path("hotels").isEmpty()).isTrue();
    assertThat(previous.path("contracts").path(0).path("canManageHotel").asBoolean()).isFalse();
    assertThat(previous.path("contracts").path(0).path("ownerId").asText())
        .isEqualTo(owner.toString());
    var current = dashboard(nextOwner, "HOTEL_OWNER");
    assertThat(current.path("ownerAmount").asLong()).isEqualTo(1760004);
    assertThat(current.path("platformAmount").asLong()).isEqualTo(240001);
    assertThat(current.path("contracts").path(0).path("canManageHotel").asBoolean()).isTrue();
    var global = dashboard(admin, "ADMIN");
    long platform = 0, owners = 0;
    for (var row : global.path("contracts")) {
      if (row.path("hotelId").asText().equals(hotel.toString())) {
        platform += row.path("platformAmount").asLong();
        owners += row.path("ownerAmount").asLong();
      }
    }
    assertThat(platform).isEqualTo(440002);
    assertThat(owners).isEqualTo(3560008);
  }

  @Test
  void shouldSeparateCollectedPaymentsFromRevenueUntilCheckout() throws Exception {
    UUID id = booking();
    pay(id);
    for (String state :
        List.of("PENDING_PAYMENT", "CONFIRMED", "CHECKED_IN", "CANCELLED", "EXPIRED", "NO_SHOW")) {
      jdbc.update("UPDATE bookings SET status=?::booking_status__t WHERE id=?", state, id);
      var report = dashboard(owner, "HOTEL_OWNER");
      assertThat(report.path("collectedAmount").asLong()).isEqualTo(2000005);
      assertThat(report.path("platformAmount").asLong()).isZero();
      assertThat(report.path("ownerAmount").asLong()).isZero();
      assertThat(report.path("contracts").isEmpty()).isTrue();
    }
  }

  @Test
  void shouldAuthorizeFinanceAgainstCurrentAccountStateInsteadOfJwtAlone() throws Exception {
    mvc.perform(get("/api/v1/finance/dashboard").with(actor(owner, "ADMIN")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/finance/dashboard").with(actor(customer, "HOTEL_OWNER")))
        .andExpect(status().isForbidden());
    jdbc.update("UPDATE users SET status='inactive' WHERE id=?", admin);
    mvc.perform(get("/api/v1/finance/dashboard").with(actor(admin, "ADMIN")))
        .andExpect(status().isForbidden());
    jdbc.update("UPDATE users SET email_verified_at=NULL WHERE id=?", owner);
    mvc.perform(get("/api/v1/finance/dashboard").with(actor(owner, "HOTEL_OWNER")))
        .andExpect(status().isForbidden());
  }

  private tools.jackson.databind.JsonNode dashboard(UUID viewer, String role) throws Exception {
    String json =
        mvc.perform(get("/api/v1/finance/dashboard").with(actor(viewer, role)))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return tools.jackson.databind.json.JsonMapper.builder().build().readTree(json).path("data");
  }

  private void pay(UUID bookingId) {
    jdbc.update(
        """
        INSERT INTO payment_transactions(id, booking_id, customer_id, amount_vnd, currency, status,
          payment_url, created_at, expires_at, booking_confirmed, paid_at)
        VALUES (?, ?, ?, 2000005, 'VND', 'SUCCEEDED', 'https://example.test/pay', ?, ?, true, ?)
        """,
        UUID.randomUUID(),
        bookingId,
        customer,
        java.sql.Timestamp.from(clock.instant()),
        java.sql.Timestamp.from(clock.instant().plusSeconds(900)),
        java.sql.Timestamp.from(clock.instant()));
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

  private String body(UUID ownerId, String state, Instant start, Instant end) {
    return """
        {"ownerUserId":"%s","commissionRate":12,"commissionBasis":"AFTER_DISCOUNT","startsAt":"%s","endsAt":"%s","status":"%s"}
        """
        .formatted(ownerId, start, end, state);
  }

  private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
          .JwtRequestPostProcessor
      actor(UUID id, String role) {
    return jwt()
        .jwt(token -> token.subject(id.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }
}
