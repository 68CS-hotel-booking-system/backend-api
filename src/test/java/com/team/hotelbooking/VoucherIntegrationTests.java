package com.team.hotelbooking;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.team.hotelbooking.booking.BookingCheckoutDirectory;
import com.team.hotelbooking.booking.application.command.CreateBookingCommand;
import com.team.hotelbooking.booking.application.command.CreateBookingCommand.*;
import com.team.hotelbooking.booking.application.usecase.CreateBookingUseCase;
import com.team.hotelbooking.booking.domain.exception.*;
import com.team.hotelbooking.pricing.voucher.application.command.*;
import com.team.hotelbooking.pricing.voucher.application.result.VoucherResult;
import com.team.hotelbooking.pricing.voucher.application.usecase.*;
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
import org.springframework.test.web.servlet.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Voucher khách sạn, giữ lượt và chốt số tiền thực trả")
class VoucherIntegrationTests {
  static final Instant NOW = Instant.parse("2027-01-01T00:00:00Z");
  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;
  @Autowired CreateBookingUseCase create;
  @Autowired CreateVoucherUseCase vouchers;
  @Autowired SetVoucherEnabledUseCase toggle;
  @Autowired BookingCheckoutDirectory checkout;
  @MockitoBean Clock clock;
  UUID owner, admin, customer, hotel, roomType;

  @BeforeEach
  void setup() {
    given(clock.instant()).willReturn(NOW);
    given(clock.getZone()).willReturn(ZoneOffset.UTC);
    given(clock.withZone(org.mockito.ArgumentMatchers.any()))
        .willAnswer(i -> Clock.fixed(clock.instant(), i.getArgument(0)));
    owner = user("HOTEL_OWNER");
    admin = user("ADMIN");
    customer = user("CUSTOMER");
    hotel = UUID.randomUUID();
    roomType = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO hotels(id,owner_user_id,city_id,name,slug,address,status) VALUES (?,?,'822cafba-73f6-4ec0-bae7-71804535cae3','Voucher hotel',?,'Da Nang','ACTIVE')",
        hotel,
        owner,
        "voucher-" + hotel);
    jdbc.update(
        "INSERT INTO room_types(id,hotel_id,name,max_occupancy,max_adults,max_children,base_price,is_active) VALUES (?,?,'Voucher room',2,2,0,2000005,TRUE)",
        roomType,
        hotel);
    for (int n = 0; n < 3; n++)
      jdbc.update(
          "INSERT INTO rooms(id,hotel_id,room_type_id,room_number,readiness,operational_from) VALUES (?,?,?,?,'READY','2026-01-01T00:00:00Z')",
          UUID.randomUUID(),
          hotel,
          roomType,
          "V" + n);
    jdbc.update(
        "INSERT INTO hotel_contracts(id,contract_code,hotel_id,owner_user_id,commission_rate,commission_basis,starts_at,ends_at,status,created_by_user_id) VALUES (?,?,?,?,10,'AFTER_DISCOUNT',?,?,'ACTIVE',?)",
        UUID.randomUUID(),
        "CT-" + hotel,
        hotel,
        owner,
        java.sql.Timestamp.from(NOW.minusSeconds(3600)),
        java.sql.Timestamp.from(NOW.plusSeconds(86400)),
        admin);
  }

  @Test
  void ownerCanCreatePauseAndResumeButCannotOverwriteWithStaleVersion() throws Exception {
    String json =
        mvc.perform(
                post(path())
                    .with(actor(owner, "HOTEL_OWNER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body(" save10 ", "PERCENT", "10", 100000, 0, 10)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.data.code").value("SAVE10"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID id = UUID.fromString(tree(json).path("id").asText());
    mvc.perform(
            patch(path() + "/" + id)
                .with(actor(owner, "HOTEL_OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false,\"expectedVersion\":0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("PAUSED"));
    mvc.perform(
            patch(path() + "/" + id)
                .with(actor(owner, "HOTEL_OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":true,\"expectedVersion\":0}"))
        .andExpect(status().isConflict());
    toggle.execute(new SetVoucherEnabledCommand(owner, hotel, id, true, 1));
    mvc.perform(get(path()).with(actor(owner, "HOTEL_OWNER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].version").value(2));
  }

  @Test
  void actualOwnerAndVerifiedActiveRoleAreRequiredEvenWithForgedJwt() throws Exception {
    for (UUID id : List.of(admin, customer, user("HOTEL_OWNER")))
      mvc.perform(
              post(path())
                  .with(actor(id, "HOTEL_OWNER"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body("SAVE10", "PERCENT", "10", null, 0, 5)))
          .andExpect(status().isForbidden());
    mvc.perform(get(path()).with(actor(admin, "ADMIN"))).andExpect(status().isForbidden());
    jdbc.update("UPDATE users SET email_verified_at=NULL WHERE id=?", owner);
    mvc.perform(get(path()).with(actor(owner, "HOTEL_OWNER"))).andExpect(status().isForbidden());
    jdbc.update("UPDATE users SET status='inactive',email_verified_at=now() WHERE id=?", owner);
    mvc.perform(get(path()).with(actor(owner, "HOTEL_OWNER"))).andExpect(status().isForbidden());
  }

  @Test
  void duplicateCodesAndFractionalMoneyAreRejected() throws Exception {
    voucher("SAVE10", "PERCENT", 10, null, 0, 10);
    mvc.perform(
            post(path())
                .with(actor(owner, "HOTEL_OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("save10", "PERCENT", "10", null, 0, 10)))
        .andExpect(status().isConflict());
    for (String value : List.of("10.5", "0", "100"))
      mvc.perform(
              post(path())
                  .with(actor(owner, "HOTEL_OWNER"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body("OTHER", "PERCENT", value, null, 0, 10)))
          .andExpect(status().isBadRequest());
    mvc.perform(
            post(path())
                .with(actor(owner, "HOTEL_OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("BAD", "FIXED", "1000", 5000, 0, 10)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void publicQuoteUsesRoomPricePercentFloorCapAndMinimum() throws Exception {
    voucher("SAVE10", "PERCENT", 10, 100000L, 0, 10);
    quote("save10")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.discountAmount").value(100000))
        .andExpect(jsonPath("$.data.totalAmount").value(1900005));
    voucher("FLOOR", "PERCENT", 10, null, 0, 10);
    quote("FLOOR").andExpect(jsonPath("$.data.discountAmount").value(200000));
    voucher("MINIMUM", "FIXED", 100000, null, 3000000, 10);
    quote("MINIMUM").andExpect(status().isBadRequest());
    quote("UNKNOWN").andExpect(status().isBadRequest());
    voucher("FIXED", "FIXED", 3000000, null, 0, 10);
    quote("FIXED")
        .andExpect(jsonPath("$.data.discountAmount").value(2000004))
        .andExpect(jsonPath("$.data.totalAmount").value(1));
  }

  @Test
  void wrongHotelFutureExpiredPausedAndEmptyCodesBehaveCorrectly() throws Exception {
    var v = voucher("SAVE10", "PERCENT", 10, null, 0, 10);
    toggle.execute(new SetVoucherEnabledCommand(owner, hotel, v.id(), false, 0));
    quote("SAVE10").andExpect(status().isConflict());
    toggle.execute(new SetVoucherEnabledCommand(owner, hotel, v.id(), true, 1));
    jdbc.update(
        "UPDATE hotel_vouchers SET starts_at=?,ends_at=? WHERE id=?",
        java.sql.Timestamp.from(NOW.plusSeconds(10)),
        java.sql.Timestamp.from(NOW.plusSeconds(20)),
        v.id());
    quote("SAVE10").andExpect(status().isConflict());
    given(clock.instant()).willReturn(NOW.plusSeconds(20));
    quote("SAVE10").andExpect(status().isConflict());
    quote(null).andExpect(status().isOk()).andExpect(jsonPath("$.data.discountAmount").value(0));
  }

  @Test
  void bookingSnapshotsDiscountAndCommissionAndDoesNotExposeInternalCommission() throws Exception {
    voucher("SAVE10", "PERCENT", 10, null, 0, 10);
    UUID id = create.execute(command("SAVE10", UUID.randomUUID().toString(), 1)).id();
    assertThat(jdbc.queryForObject("SELECT total_amount FROM bookings WHERE id=?", Long.class, id))
        .isEqualTo(1800005);
    assertThat(
            jdbc.queryForObject(
                "SELECT platform_amount FROM booking_commissions WHERE booking_id=?",
                Long.class,
                id))
        .isEqualTo(180001);
    assertThat(
            jdbc.queryForObject(
                "SELECT owner_amount FROM booking_commissions WHERE booking_id=?", Long.class, id))
        .isEqualTo(1620004);
    assertThat(
            jdbc.queryForObject(
                "SELECT discount_amount FROM voucher_redemptions WHERE booking_id=?",
                Long.class,
                id))
        .isEqualTo(200000);
    mvc.perform(get("/api/v1/bookings/" + id).with(actor(customer, "CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.discountAmount").value(200000))
        .andExpect(jsonPath("$.data.platformAmount").doesNotExist());
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE voucher_redemptions SET discount_amount=100 WHERE booking_id=?", id))
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
  }

  @Test
  void replayDoesNotUseAnotherSlotAndDifferentCodeWithSameDiscountConflicts() {
    var v = voucher("SAVE10", "PERCENT", 10, null, 0, 1);
    voucher("OTHER", "PERCENT", 10, null, 0, 1);
    String key = UUID.randomUUID().toString();
    var c = command("SAVE10", key, 1);
    UUID id = create.execute(c).id();
    toggle.execute(new SetVoucherEnabledCommand(owner, hotel, v.id(), false, 0));
    assertThat(create.execute(c).id()).isEqualTo(id);
    assertThatThrownBy(() -> create.execute(command("OTHER", key, 1)))
        .isInstanceOfSatisfying(
            BookingException.class,
            e -> assertThat(e.code()).isEqualTo(BookingErrorCode.IDEMPOTENCY_CONFLICT));
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM voucher_redemptions WHERE voucher_id=?", Long.class, v.id()))
        .isEqualTo(1);
  }

  @Test
  void concurrentLastSlotAllowsOnlyOneBookingAndRollsBackLoser() throws Exception {
    var v = voucher("SAVE10", "PERCENT", 10, null, 0, 1);
    var gate = new java.util.concurrent.CyclicBarrier(2);
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var tasks = new ArrayList<java.util.concurrent.Future<Boolean>>();
      for (int i = 0; i < 2; i++)
        tasks.add(
            pool.submit(
                () -> {
                  gate.await();
                  try {
                    create.execute(command("SAVE10", UUID.randomUUID().toString(), 1));
                    return true;
                  } catch (BookingException e) {
                    assertThat(e.code()).isEqualTo(BookingErrorCode.VOUCHER_UNAVAILABLE);
                    return false;
                  }
                }));
      assertThat(
              tasks.get(0).get(30, java.util.concurrent.TimeUnit.SECONDS)
                  ^ tasks.get(1).get(30, java.util.concurrent.TimeUnit.SECONDS))
          .isTrue();
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM voucher_redemptions WHERE voucher_id=?", Long.class, v.id()))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM bookings WHERE hotel_id=?", Long.class, hotel))
        .isEqualTo(1);
  }

  @Test
  void unpaidCancellationReturnsSlotImmediately() throws Exception {
    var v = voucher("SAVE10", "PERCENT", 10, null, 0, 1);
    UUID first = create.execute(command("SAVE10", null, 1)).id();
    quote("SAVE10").andExpect(status().isConflict());
    mvc.perform(post("/api/v1/bookings/" + first + "/cancel").with(actor(customer, "CUSTOMER")))
        .andExpect(status().isOk());
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM voucher_redemptions WHERE booking_id=?", String.class, first))
        .isEqualTo("RELEASED");
    create.execute(command("SAVE10", null, 1));
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM voucher_redemptions WHERE voucher_id=?", Long.class, v.id()))
        .isEqualTo(2);
  }

  @Test
  void expiredReservationReturnsSlotButCannotConfirmLatePayment() {
    voucher("SAVE10", "PERCENT", 10, null, 0, 1);
    UUID first = create.execute(command("SAVE10", null, 1)).id();
    given(clock.instant()).willReturn(NOW.plusSeconds(901));
    create.execute(command("SAVE10", null, 1));
    assertThat(checkout.confirmPayment(first, NOW)).isFalse();
  }

  @Test
  void paidCancellationRefundsOnlyActualPaymentAndDoesNotReturnUsedSlot() throws Exception {
    voucher("SAVE10", "PERCENT", 10, null, 0, 1);
    UUID id = create.execute(command("SAVE10", null, 1)).id();
    assertThat(checkout.confirmPayment(id, NOW)).isTrue();
    jdbc.update(
        "INSERT INTO payment_transactions(id,booking_id,customer_id,amount_vnd,currency,status,payment_url,created_at,expires_at,booking_confirmed,paid_at) VALUES (?,?,?,1800005,'VND','SUCCEEDED','https://example.test/pay',?,?,true,?)",
        UUID.randomUUID(),
        id,
        customer,
        java.sql.Timestamp.from(NOW),
        java.sql.Timestamp.from(NOW.plusSeconds(900)),
        java.sql.Timestamp.from(NOW));
    String json =
        mvc.perform(
                get("/api/v1/bookings/" + id + "/cancellation").with(actor(customer, "CUSTOMER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.refundAmount").value(1800005))
            .andReturn()
            .getResponse()
            .getContentAsString();
    long version = tree(json).path("expectedVersion").asLong();
    mvc.perform(
            post("/api/v1/bookings/" + id + "/cancel")
                .with(actor(customer, "CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":" + version + ",\"expectedRefundAmount\":1800005}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.cancellation.refundAmount").value(1800005));
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM voucher_redemptions WHERE booking_id=?", String.class, id))
        .isEqualTo("USED");
    quote("SAVE10").andExpect(status().isConflict());
  }

  @Test
  void inventoryFailureRollsBackReservationAndPriceChangeRejectsTamperedTotal() {
    var v = voucher("SAVE10", "PERCENT", 10, null, 0, 5);
    assertThatThrownBy(() -> create.execute(command("SAVE10", null, 4)))
        .isInstanceOf(RuntimeException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM voucher_redemptions WHERE voucher_id=?", Long.class, v.id()))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM bookings WHERE hotel_id=?", Long.class, hotel))
        .isZero();
    var c = command("SAVE10", null, 1);
    var tampered =
        new CreateBookingCommand(
            c.customerId(),
            c.hotelId(),
            c.checkIn(),
            c.checkOut(),
            c.expectedCheckInTime(),
            c.expectedCheckOutTime(),
            c.booker(),
            c.guest(),
            c.items(),
            1L,
            null,
            "SAVE10");
    assertThatThrownBy(() -> create.execute(tampered))
        .isInstanceOfSatisfying(
            BookingException.class,
            e -> assertThat(e.code()).isEqualTo(BookingErrorCode.PRICE_CHANGED));
  }

  @Test
  void transferredOwnerLosesManagementRights() throws Exception {
    var v = voucher("SAVE10", "PERCENT", 10, null, 0, 10);
    UUID next = user("HOTEL_OWNER");
    jdbc.update("UPDATE hotels SET owner_user_id=? WHERE id=?", next, hotel);
    mvc.perform(get(path()).with(actor(owner, "HOTEL_OWNER"))).andExpect(status().isForbidden());
    mvc.perform(get(path()).with(actor(next, "HOTEL_OWNER"))).andExpect(status().isOk());
    toggle.execute(new SetVoucherEnabledCommand(next, hotel, v.id(), false, 0));
  }

  private VoucherResult voucher(
      String code, String type, long value, Long cap, long min, int limit) {
    return vouchers.execute(
        new CreateVoucherCommand(
            owner,
            hotel,
            code,
            "Ưu đãi lưu trú",
            type,
            value,
            cap,
            min,
            NOW.minusSeconds(1),
            NOW.plusSeconds(3600),
            limit));
  }

  private CreateBookingCommand command(String code, String key, int qty) {
    return new CreateBookingCommand(
        customer,
        hotel,
        LocalDate.of(2027, 1, 10),
        LocalDate.of(2027, 1, 11),
        null,
        null,
        new ContactInput("Khách test", "guest@example.test", "+84980008881"),
        null,
        List.of(new BookingItemInput(roomType, qty, qty, 0)),
        null,
        key,
        code);
  }

  private ResultActions quote(String code) throws Exception {
    return mvc.perform(
        post("/api/v1/hotels/" + hotel + "/quote")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"checkIn\":\"2027-01-10\",\"checkOut\":\"2027-01-11\",\"items\":[{\"roomTypeId\":\""
                    + roomType
                    + "\",\"quantity\":1,\"adults\":1,\"children\":0}]"
                    + (code == null ? "" : ",\"voucherCode\":\"" + code + "\"")
                    + "}"));
  }

  private String path() {
    return "/api/v1/management/hotels/" + hotel + "/vouchers";
  }

  private String body(String code, String type, String value, Number cap, long minimum, int limit) {
    return "{\"code\":\""
        + code
        + "\",\"name\":\"Ưu đãi\",\"discountType\":\""
        + type
        + "\",\"discountValue\":"
        + value
        + ",\"maxDiscountAmount\":"
        + cap
        + ",\"minOrderAmount\":"
        + minimum
        + ",\"startsAt\":\""
        + NOW.minusSeconds(1)
        + "\",\"endsAt\":\""
        + NOW.plusSeconds(3600)
        + "\",\"usageLimit\":"
        + limit
        + "}";
  }

  private UUID user(String role) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO users(id,email,password_hash,full_name,phone,status,email_verified_at) VALUES (?,?,'unused','Test',?,'active',now())",
        id,
        id + "@voucher.test",
        "+" + (100000000000L + Math.abs(id.hashCode())));
    jdbc.update(
        "INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE role_key=?",
        id,
        role);
    return id;
  }

  private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
          .JwtRequestPostProcessor
      actor(UUID id, String role) {
    return jwt()
        .jwt(t -> t.subject(id.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  private tools.jackson.databind.JsonNode tree(String json) throws Exception {
    return tools.jackson.databind.json.JsonMapper.builder().build().readTree(json).path("data");
  }
}
