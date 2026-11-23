package com.team.hotelbooking;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.team.hotelbooking.booking.application.command.CreateBookingCommand;
import com.team.hotelbooking.booking.application.command.CreateBookingCommand.BookingItemInput;
import com.team.hotelbooking.booking.application.command.CreateBookingCommand.ContactInput;
import com.team.hotelbooking.booking.application.usecase.CreateBookingUseCase;
import com.team.hotelbooking.booking.infrastructure.integration.BookingEmailNotifications;
import com.team.hotelbooking.payment.application.port.VnpayGateway;
import com.team.hotelbooking.payment.application.service.VnpayPaymentService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
    properties = {
      "payment.vnpay.enabled=true",
      "payment.vnpay.tmn-code=TESTCODE",
      "payment.vnpay.hash-secret=sandbox-test-secret",
      "payment.vnpay.payment-url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
      "payment.vnpay.return-url=http://localhost:5173/payment/return",
      "booking.email-poll-delay=PT24H"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VnpayPaymentIntegrationTests {
  private static PostgreSQLContainer postgres;
  private static final UUID CUSTOMER = UUID.fromString("90000000-0000-4000-8000-000000000001");
  private static final UUID HOTEL = UUID.fromString("90000000-0000-4000-8000-000000000002");
  private static final UUID ROOM_TYPE = UUID.fromString("90000000-0000-4000-8000-000000000003");
  private static final UUID ROOM = UUID.fromString("90000000-0000-4000-8000-000000000004");
  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;
  @Autowired CreateBookingUseCase createBooking;
  @Autowired VnpayPaymentService payments;
  @Autowired BookingEmailNotifications notifications;
  @MockitoBean JavaMailSender mail;
  @MockitoSpyBean VnpayGateway gateway;

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry registry) {
    String externalUrl = System.getProperty("test.postgres.url");
    if (externalUrl != null && !externalUrl.isBlank()) {
      registry.add("spring.datasource.url", () -> externalUrl);
      registry.add(
          "spring.datasource.username",
          () -> System.getProperty("test.postgres.username", "postgres"));
      registry.add(
          "spring.datasource.password", () -> System.getProperty("test.postgres.password", ""));
    } else {
      postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17"));
      postgres.start();
      registry.add("spring.datasource.url", postgres::getJdbcUrl);
      registry.add("spring.datasource.username", postgres::getUsername);
      registry.add("spring.datasource.password", postgres::getPassword);
    }
  }

  @BeforeEach
  void seed() {
    jdbc.update("DELETE FROM payment_transactions WHERE customer_id = ?", CUSTOMER);
    jdbc.update("DELETE FROM inventory_reservations WHERE room_type_id = ?", ROOM_TYPE);
    jdbc.update("DELETE FROM bookings WHERE hotel_id = ?", HOTEL);
    jdbc.update(
        """
        INSERT INTO users(id, email, password_hash, full_name, phone, status, email_verified_at)
        VALUES (?, 'vnpay-customer@test.local', 'unused', 'Khach VNPAY', '+84980008881', 'active', now()) ON CONFLICT(id) DO NOTHING
        """,
        CUSTOMER);
    jdbc.update(
        """
        INSERT INTO hotels(id, owner_user_id, city_id, name, slug, address, star_rating, status)
        VALUES (?, ?, '822cafba-73f6-4ec0-bae7-71804535cae3', 'VNPAY test hotel', 'vnpay-test-hotel', 'Da Nang', 3, 'ACTIVE') ON CONFLICT(id) DO NOTHING
        """,
        HOTEL,
        CUSTOMER);
    jdbc.update(
        """
        INSERT INTO room_types(id, hotel_id, name, max_occupancy, max_adults, max_children, base_price, is_active)
        VALUES (?, ?, 'VNPAY test room', 2, 2, 0, 123456, TRUE) ON CONFLICT(id) DO NOTHING
        """,
        ROOM_TYPE,
        HOTEL);
    jdbc.update(
        """
        INSERT INTO rooms(id, hotel_id, room_type_id, room_number, readiness, operational_from)
        VALUES (?, ?, ?, 'VNP01', 'READY', '2026-01-01T00:00:00Z') ON CONFLICT(id) DO NOTHING
        """,
        ROOM,
        HOTEL,
        ROOM_TYPE);
  }

  private UUID booking() {
    LocalDate checkIn = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(10);
    return createBooking
        .execute(
            new CreateBookingCommand(
                CUSTOMER,
                HOTEL,
                checkIn,
                checkIn.plusDays(1),
                LocalTime.of(14, 0),
                LocalTime.of(12, 0),
                new ContactInput("Khach test", "vnpay-customer@test.local", "+84980008881"),
                new ContactInput("Khach luu tru", "guest@test.local", "+84980008882"),
                List.of(new BookingItemInput(ROOM_TYPE, 1, 2, 0)),
                123456L))
        .id();
  }

  private MultiValueMap<String, String> ipn(UUID id, String response, String status)
      throws Exception {
    Map<String, String> fields = new TreeMap<>();
    fields.put("vnp_TmnCode", "TESTCODE");
    fields.put("vnp_TxnRef", id.toString());
    fields.put("vnp_Amount", "12345600");
    fields.put("vnp_ResponseCode", response);
    fields.put("vnp_TransactionStatus", status);
    fields.put("vnp_TransactionNo", "123456789");
    fields.put("vnp_OrderInfo", "Thanh toan dat phong");
    fields.put(
        "vnp_PayDate",
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"))
            .format(Instant.now()));
    return signed(fields);
  }

  private MultiValueMap<String, String> signed(Map<String, String> fields) throws Exception {
    String data =
        new TreeMap<>(fields)
            .entrySet().stream()
                .map(
                    e ->
                        URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                            + "="
                            + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    Mac mac = Mac.getInstance("HmacSHA512");
    mac.init(
        new SecretKeySpec("sandbox-test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
    MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
    fields.forEach(result::add);
    result.add(
        "vnp_SecureHash",
        HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8))));
    return result;
  }

  private String bookingStatus(UUID id) {
    return jdbc.queryForObject("SELECT status::text FROM bookings WHERE id = ?", String.class, id);
  }

  private int confirmedReservations(UUID id) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM inventory_reservations WHERE booking_id = ? AND status = 'CONFIRMED'",
        Integer.class,
        id);
  }

  private int confirmations(UUID id) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM booking_status_history WHERE booking_id = ? AND to_status = 'CONFIRMED'",
        Integer.class,
        id);
  }

  private int queuedEmails(UUID id) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM booking_email_notifications WHERE booking_id = ?", Integer.class, id);
  }

  private void makeEmailDue(UUID id, boolean reminder) {
    jdbc.update(
        """
        UPDATE booking_email_notifications SET due_at = now() - interval '1 minute'
        WHERE booking_id = ? AND is_reminder = ?
        """,
        id,
        reminder);
  }

  @Test
  void authenticatedCreateAndReadThenPublicIpnConfirmBookingAndInventoryExactlyOnce()
      throws Exception {
    UUID booking = booking();
    var payment = payments.create(CUSTOMER, booking, "127.0.0.1");
    mvc.perform(
            post("/api/v1/payments/bookings/{id}/vnpay", booking)
                .with(jwt().jwt(j -> j.subject(CUSTOMER.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(payment.id().toString()))
        .andExpect(jsonPath("$.data.amountVnd").value(123456))
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andExpect(header().string("Cache-Control", "no-store"));
    var parameters = ipn(payment.id(), "00", "00");
    mvc.perform(get("/api/v1/payments/vnpay/ipn").queryParams(parameters))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.RspCode").value("00"));
    mvc.perform(
            get("/api/v1/payments/{id}", payment.id())
                .with(jwt().jwt(j -> j.subject(CUSTOMER.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
        .andExpect(jsonPath("$.data.bookingConfirmed").value(true))
        .andExpect(jsonPath("$.data.paymentUrl").isEmpty());
    mvc.perform(get("/api/v1/payments/vnpay/ipn").queryParams(parameters))
        .andExpect(jsonPath("$.RspCode").value("02"));
    assertThat(bookingStatus(booking)).isEqualTo("CONFIRMED");
    assertThat(confirmedReservations(booking)).isEqualTo(1);
    assertThat(confirmations(booking)).isEqualTo(1);
    assertThat(queuedEmails(booking)).isEqualTo(2);
  }

  @Test
  @DisplayName("Gửi xác nhận đúng số tiền và nhắc đúng người sau khi giao dịch commit")
  void shouldSendConfirmationAndReminderToTheirRecipients() throws Exception {
    // Given
    UUID booking = booking();
    var payment = payments.create(CUSTOMER, booking, "127.0.0.1");
    assertThat(payments.receiveIpn(ipn(payment.id(), "00", "00")).code()).isEqualTo("00");
    var expectedReminder =
        LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))
            .plusDays(9)
            .atTime(14, 0)
            .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
            .toInstant();
    assertThat(
            jdbc.queryForObject(
                    "SELECT due_at FROM booking_email_notifications WHERE booking_id = ? AND is_reminder",
                    (rs, row) -> rs.getObject(1, OffsetDateTime.class),
                    booking)
                .toInstant())
        .isEqualTo(expectedReminder);
    var messages = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);

    // When
    notifications.deliverDueEmails();

    // Then
    verify(mail).send(messages.capture());
    assertThat(messages.getValue().getTo()).containsExactly("vnpay-customer@test.local");
    assertThat(messages.getValue().getText()).contains("Đã thanh toán: 123456 VND");
    assertThat(queuedEmails(booking)).isEqualTo(1);

    makeEmailDue(booking, true);
    notifications.deliverDueEmails();
    notifications.deliverDueEmails();
    verify(mail, times(2)).send(messages.capture());
    assertThat(messages.getValue().getTo()).containsExactly("guest@test.local");
    assertThat(messages.getValue().getSubject()).contains("Nhắc nhận phòng");
    assertThat(queuedEmails(booking)).isZero();
  }

  @Test
  @DisplayName("Giữ email để thử lại khi SMTP lỗi và bỏ nhắc của đơn đã hủy")
  void shouldRetryFailedEmailAndSkipReminderWhenBookingIsCancelled() throws Exception {
    // Given
    UUID booking = booking();
    var payment = payments.create(CUSTOMER, booking, "127.0.0.1");
    payments.receiveIpn(ipn(payment.id(), "00", "00"));
    doThrow(new MailSendException("SMTP unavailable"))
        .doNothing()
        .when(mail)
        .send(any(SimpleMailMessage.class));

    // When
    notifications.deliverDueEmails();
    notifications.deliverDueEmails();

    // Then
    verify(mail).send(any(SimpleMailMessage.class));
    assertThat(queuedEmails(booking)).isEqualTo(2);
    assertThat(bookingStatus(booking)).isEqualTo("CONFIRMED");
    assertThat(payments.get(CUSTOMER, payment.id()).status()).isEqualTo("SUCCEEDED");

    makeEmailDue(booking, false);
    notifications.deliverDueEmails();
    jdbc.update("UPDATE bookings SET status = 'CANCELLED' WHERE id = ?", booking);
    makeEmailDue(booking, true);
    notifications.deliverDueEmails();
    verify(mail, times(2)).send(any(SimpleMailMessage.class));
    assertThat(queuedEmails(booking)).isZero();
  }

  @Test
  void ownerIsolationAndPublicCallbackWithoutSignature() throws Exception {
    UUID booking = booking();
    var payment = payments.create(CUSTOMER, booking, "127.0.0.1");
    mvc.perform(post("/api/v1/payments/bookings/{id}/vnpay", booking))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/payments/{id}", payment.id())).andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/v1/payments/{id}", payment.id())
                .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/v1/payments/bookings/{id}/vnpay", booking)
                .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/payments/vnpay/ipn"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.RspCode").value("97"));
  }

  @Test
  void ownedQrIsPersistedReusedAndConfirmedOnlyBySignedIpn() throws Exception {
    UUID booking = booking();
    doReturn("000201-test-qr")
        .when(gateway)
        .createQr(any(), anyLong(), anyString(), any(), any(), anyString());
    mvc.perform(post("/api/v1/payments/bookings/{id}/vnpay", booking).param("qr", "true"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/v1/payments/bookings/{id}/vnpay", booking)
                .param("qr", "true")
                .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))))
        .andExpect(status().isNotFound());
    for (int repeat = 0; repeat < 2; repeat++) {
      mvc.perform(
              post("/api/v1/payments/bookings/{id}/vnpay", booking)
                  .param("qr", "true")
                  .with(jwt().jwt(j -> j.subject(CUSTOMER.toString()))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.qrContent").value("000201-test-qr"))
          .andExpect(jsonPath("$.data.status").value("PENDING"));
    }
    verify(gateway, times(1)).createQr(any(), anyLong(), anyString(), any(), any(), anyString());
    var payment = payments.create(CUSTOMER, booking, "127.0.0.1");
    assertThat(
            jdbc.queryForObject(
                "SELECT qr_content FROM payment_transactions WHERE id = ?",
                String.class,
                payment.id()))
        .isEqualTo("000201-test-qr");
    assertThat(bookingStatus(booking)).isEqualTo("PENDING_PAYMENT");
    mvc.perform(get("/api/v1/payments/vnpay/ipn").queryParams(ipn(payment.id(), "00", "00")))
        .andExpect(jsonPath("$.RspCode").value("00"));
    assertThat(payments.get(CUSTOMER, payment.id()).qrContent()).isNull();
    assertThat(bookingStatus(booking)).isEqualTo("CONFIRMED");
  }

  @Test
  void tamperingWrongMerchantAndWrongAmountNeverConfirm() throws Exception {
    UUID booking = booking();
    var payment = payments.create(CUSTOMER, booking, "127.0.0.1");
    var parameters = ipn(payment.id(), "00", "00");
    parameters.set("vnp_Amount", "1");
    assertThat(payments.receiveIpn(parameters).code()).isEqualTo("97");
    var fields = ipn(payment.id(), "00", "00").toSingleValueMap();
    fields.remove("vnp_SecureHash");
    fields.put("vnp_Amount", "12345601");
    assertThat(payments.receiveIpn(signed(fields)).code()).isEqualTo("04");
    fields.put("vnp_TmnCode", "OTHER");
    assertThat(payments.receiveIpn(signed(fields)).code()).isEqualTo("97");
    assertThat(payments.get(CUSTOMER, payment.id()).status()).isEqualTo("PENDING");
    assertThat(bookingStatus(booking)).isEqualTo("PENDING_PAYMENT");
    assertThat(confirmedReservations(booking)).isZero();
    assertThat(queuedEmails(booking)).isZero();
  }

  @Test
  void cancelledPaymentCanBeRetriedBeforeBookingDeadline() throws Exception {
    UUID booking = booking();
    var first = payments.create(CUSTOMER, booking, "127.0.0.1");
    assertThat(payments.receiveIpn(ipn(first.id(), "24", "02")).code()).isEqualTo("00");
    assertThat(payments.get(CUSTOMER, first.id()).status()).isEqualTo("FAILED");
    assertThat(queuedEmails(booking)).isZero();
    var second = payments.create(CUSTOMER, booking, "127.0.0.1");
    assertThat(second.id()).isNotEqualTo(first.id());
    assertThat(payments.receiveIpn(ipn(second.id(), "00", "00")).code()).isEqualTo("00");
    assertThat(bookingStatus(booking)).isEqualTo("CONFIRMED");
  }

  @Test
  void lateSuccessIsSavedForReviewWithoutTakingExpiredInventory() throws Exception {
    UUID booking = booking();
    var payment = payments.create(CUSTOMER, booking, "127.0.0.1");
    jdbc.update(
        "UPDATE bookings SET expires_at = now() - interval '1 minute' WHERE id = ?", booking);
    jdbc.update(
        "UPDATE inventory_reservations SET expires_at = now() - interval '1 minute' WHERE booking_id = ?",
        booking);
    assertThat(payments.receiveIpn(ipn(payment.id(), "00", "00")).code()).isEqualTo("00");
    var result = payments.get(CUSTOMER, payment.id());
    assertThat(result.status()).isEqualTo("SUCCEEDED");
    assertThat(result.bookingConfirmed()).isFalse();
    assertThat(bookingStatus(booking)).isEqualTo("PENDING_PAYMENT");
    assertThat(confirmedReservations(booking)).isZero();
    assertThat(queuedEmails(booking)).isZero();
  }

  @Test
  void concurrentCreatesReuseOnePaymentAndConcurrentIpnsConfirmOnce() throws Exception {
    UUID booking = booking();
    try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
      List<Callable<UUID>> createCalls = new ArrayList<>();
      for (int i = 0; i < 8; i++)
        createCalls.add(() -> payments.create(CUSTOMER, booking, "127.0.0.1").id());
      Set<UUID> ids = new HashSet<>();
      for (var future : pool.invokeAll(createCalls)) ids.add(future.get(20, TimeUnit.SECONDS));
      assertThat(ids).hasSize(1);
      var parameters = ipn(ids.iterator().next(), "00", "00");
      List<Callable<String>> ipnCalls = new ArrayList<>();
      for (int i = 0; i < 8; i++) ipnCalls.add(() -> payments.receiveIpn(parameters).code());
      List<String> results = new ArrayList<>();
      for (var future : pool.invokeAll(ipnCalls)) results.add(future.get(20, TimeUnit.SECONDS));
      assertThat(Collections.frequency(results, "00")).isEqualTo(1);
      assertThat(Collections.frequency(results, "02")).isEqualTo(7);
    }
    assertThat(confirmations(booking)).isEqualTo(1);
    assertThat(confirmedReservations(booking)).isEqualTo(1);
    assertThat(queuedEmails(booking)).isEqualTo(2);
  }

  @Test
  void databaseFailureRollsBackPaymentBookingAndInventoryAndIpnCanRetry() throws Exception {
    UUID booking = booking();
    var payment = payments.create(CUSTOMER, booking, "127.0.0.1");
    var parameters = ipn(payment.id(), "00", "00");
    jdbc.execute(
        """
        CREATE FUNCTION vnpay_test_reject_confirmation() RETURNS trigger LANGUAGE plpgsql AS $$
        BEGIN
          IF NEW.booking_confirmed THEN RAISE EXCEPTION 'simulated confirmation failure'; END IF;
          RETURN NEW;
        END $$;
        CREATE TRIGGER vnpay_test_failure BEFORE UPDATE ON payment_transactions
        FOR EACH ROW EXECUTE FUNCTION vnpay_test_reject_confirmation();
        """);
    try {
      mvc.perform(get("/api/v1/payments/vnpay/ipn").queryParams(parameters))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.RspCode").value("99"));
      assertThat(payments.get(CUSTOMER, payment.id()).status()).isEqualTo("PENDING");
      assertThat(bookingStatus(booking)).isEqualTo("PENDING_PAYMENT");
      assertThat(confirmedReservations(booking)).isZero();
      assertThat(queuedEmails(booking)).isZero();
    } finally {
      jdbc.execute(
          "DROP TRIGGER vnpay_test_failure ON payment_transactions; DROP FUNCTION vnpay_test_reject_confirmation();");
    }
    assertThat(payments.receiveIpn(parameters).code()).isEqualTo("00");
    assertThat(bookingStatus(booking)).isEqualTo("CONFIRMED");
    assertThat(queuedEmails(booking)).isEqualTo(2);
  }
}
