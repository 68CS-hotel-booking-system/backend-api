package com.team.hotelbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.hotelbooking.booking.complaint.application.command.SubmitComplaintCommand;
import com.team.hotelbooking.booking.complaint.application.usecase.SubmitComplaintUseCase;
import com.team.hotelbooking.booking.complaint.domain.exception.ComplaintException;
import com.team.hotelbooking.booking.complaint.domain.model.Complaint;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

// Đặt ở package gốc vì TestcontainersConfiguration là package-private.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Khiếu nại của khách trên PostgreSQL (BK-21, tuần 1)")
class ComplaintIntegrationTests {

  private static final UUID CUSTOMER_ID = UUID.fromString("8c000000-0000-4000-8000-000000000001");
  private static final UUID OTHER_CUSTOMER_ID =
      UUID.fromString("8c000000-0000-4000-8000-000000000002");
  private static final UUID OWNER_ID = UUID.fromString("8c000000-0000-4000-8000-000000000003");
  private static final UUID HOTEL_ID = UUID.fromString("8c000000-0000-4000-8000-000000000101");
  private static final UUID DA_NANG_ID = UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3");
  private static final String VALID_BODY =
      "{\"category\": \"ROOM_CONDITION\", \"content\": \"Phòng có mùi ẩm mốc.\"}";

  @Autowired private MockMvc mvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ObjectMapper mapper;
  @Autowired private SubmitComplaintUseCase submitComplaintUseCase;

  @BeforeEach
  void setUp() {
    insertUser(CUSTOMER_ID, "+84958000001");
    insertUser(OTHER_CUSTOMER_ID, "+84958000002");
    insertUser(OWNER_ID, "+84958000003");
    jdbc.update(
        """
        INSERT INTO hotels (id, owner_user_id, city_id, name, slug, address, status)
        VALUES (?, ?, ?, 'Khách sạn khiếu nại test', 'complaint-it-hotel', '5 Test', 'ACTIVE')
        ON CONFLICT (id) DO NOTHING
        """,
        HOTEL_ID,
        OWNER_ID,
        DA_NANG_ID);
    jdbc.update(
        "DELETE FROM complaints WHERE customer_id IN (?, ?)", CUSTOMER_ID, OTHER_CUSTOMER_ID);
  }

  private void insertUser(UUID id, String phone) {
    jdbc.update(
        """
        INSERT INTO users (id, email, password_hash, full_name, phone, status, email_verified_at)
        VALUES (?, ?, 'not-a-real-hash', 'Khách khiếu nại', ?, 'active', now())
        ON CONFLICT (id) DO NOTHING
        """,
        id,
        "complaint-it-" + id + "@test.local",
        phone);
  }

  // Mỗi test dùng booking mới để không phụ thuộc thứ tự chạy.
  private UUID insertBooking(UUID customerId, String status) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO bookings (id, booking_code, customer_id, hotel_id, booker_name_snapshot,
                              booker_email_snapshot, booker_phone_snapshot, guest_name_snapshot,
                              guest_email_snapshot, guest_phone_snapshot, check_in, check_out,
                              days, nights, status)
        VALUES (?, ?, ?, ?, 'Khách', 'khach@test.local', '+84900000000', 'Khách',
                'khach@test.local', '+84900000000', '2027-06-01', '2027-06-03', 3, 2,
                CAST(? AS booking_status__t))
        """,
        id,
        "CPL" + id.toString().substring(0, 8).toUpperCase(),
        customerId,
        HOTEL_ID,
        status);
    return id;
  }

  private static JwtRequestPostProcessor customer(UUID userId) {
    return jwt()
        .jwt(token -> token.subject(userId.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
  }

  private ResultActions submit(UUID bookingId, JwtRequestPostProcessor actor, String body)
      throws Exception {
    return mvc.perform(
        post("/api/v1/bookings/" + bookingId + "/complaints")
            .with(actor)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private String complaintIdFrom(ResultActions response) throws Exception {
    return mapper
        .readTree(response.andReturn().getResponse().getContentAsString())
        .get("data")
        .get("id")
        .asText();
  }

  @Nested
  @DisplayName("POST /api/v1/bookings/{bookingId}/complaints")
  class SubmitApiTests {

    @Test
    @DisplayName("Mọi trạng thái trừ PENDING_PAYMENT được gửi (201, OPEN)")
    void shouldAcceptEveryStatusExceptPendingPayment() throws Exception {
      for (String status :
          new String[] {
            "CONFIRMED", "CHECKED_IN", "CHECKED_OUT", "CANCELLED", "EXPIRED", "NO_SHOW"
          }) {
        UUID bookingId = insertBooking(CUSTOMER_ID, status);
        submit(bookingId, customer(CUSTOMER_ID), VALID_BODY)
            .andExpect(status().isCreated())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.message").value("Gửi khiếu nại thành công."))
            .andExpect(jsonPath("$.data.bookingId").value(bookingId.toString()))
            .andExpect(jsonPath("$.data.hotelId").value(HOTEL_ID.toString()))
            .andExpect(jsonPath("$.data.category").value("ROOM_CONDITION"))
            .andExpect(jsonPath("$.data.status").value("OPEN"));
      }
    }

    @Test
    @DisplayName("PENDING_PAYMENT trả 409 BOOKING_NOT_COMPLAINABLE")
    void shouldRejectPendingPaymentBooking() throws Exception {
      submit(insertBooking(CUSTOMER_ID, "PENDING_PAYMENT"), customer(CUSTOMER_ID), VALID_BODY)
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("BOOKING_NOT_COMPLAINABLE"))
          .andExpect(
              jsonPath("$.detail")
                  .value("Đơn đặt phòng đang chờ thanh toán, chưa thể gửi khiếu nại."));
    }

    @Test
    @DisplayName("Booking của người khác hoặc không tồn tại trả 404; chưa đăng nhập trả 401")
    void shouldHideOtherCustomersBookingAndRequireLogin() throws Exception {
      UUID othersBooking = insertBooking(OTHER_CUSTOMER_ID, "CHECKED_OUT");

      submit(othersBooking, customer(CUSTOMER_ID), VALID_BODY)
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"));
      submit(UUID.randomUUID(), customer(CUSTOMER_ID), VALID_BODY)
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"));
      mvc.perform(
              post("/api/v1/bookings/" + othersBooking + "/complaints")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(VALID_BODY))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Gửi lần 2 khi đang mở trả 409 kèm id; sau khi đóng thì gửi mới được")
    void shouldRejectSecondOpenComplaintUntilFirstClosed() throws Exception {
      // Given
      UUID bookingId = insertBooking(CUSTOMER_ID, "CHECKED_OUT");
      String firstId =
          complaintIdFrom(
              submit(bookingId, customer(CUSTOMER_ID), VALID_BODY).andExpect(status().isCreated()));

      // When / Then
      submit(bookingId, customer(CUSTOMER_ID), VALID_BODY)
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("COMPLAINT_ALREADY_OPEN"))
          .andExpect(
              jsonPath("$.detail").value("Đơn đặt phòng này đang có khiếu nại chưa xử lý xong."))
          .andExpect(jsonPath("$.openComplaintId").value(firstId));

      jdbc.update(
          "UPDATE complaints SET status = 'IN_REVIEW' WHERE id = ?", UUID.fromString(firstId));
      submit(bookingId, customer(CUSTOMER_ID), VALID_BODY)
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.openComplaintId").value(firstId));

      jdbc.update(
          "UPDATE complaints SET status = 'RESOLVED' WHERE id = ?", UUID.fromString(firstId));
      submit(bookingId, customer(CUSTOMER_ID), VALID_BODY).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Hai request đồng thời cho cùng booking: đúng một thành công, một bị 409 kèm id")
    void shouldAcceptExactlyOneWhenTwoSubmissionsRace() throws Exception {
      // Given
      UUID bookingId = insertBooking(CUSTOMER_ID, "CHECKED_OUT");
      CyclicBarrier start = new CyclicBarrier(2);
      ExecutorService pool = Executors.newFixedThreadPool(2);
      List<Future<String>> outcomes = new ArrayList<>();

      // When
      try {
        for (int i = 0; i < 2; i++) {
          outcomes.add(
              pool.submit(
                  () -> {
                    start.await(10, TimeUnit.SECONDS);
                    try {
                      return "CREATED:"
                          + submitComplaintUseCase
                              .execute(
                                  new SubmitComplaintCommand(
                                      CUSTOMER_ID,
                                      bookingId,
                                      Complaint.Category.SERVICE_QUALITY,
                                      "Lễ tân phục vụ chậm."))
                              .id();
                    } catch (ComplaintException exception) {
                      return exception.code().name() + ":" + exception.openComplaintId();
                    }
                  }));
        }
        List<String> results = new ArrayList<>();
        for (Future<String> outcome : outcomes) {
          results.add(outcome.get(30, TimeUnit.SECONDS));
        }

        // Then
        String created =
            results.stream().filter(r -> r.startsWith("CREATED:")).findFirst().orElseThrow();
        String rejected =
            results.stream()
                .filter(r -> r.startsWith("COMPLAINT_ALREADY_OPEN:"))
                .findFirst()
                .orElseThrow();
        assertThat(rejected.substring("COMPLAINT_ALREADY_OPEN:".length()))
            .isEqualTo(created.substring("CREATED:".length()));
        assertThat(
                jdbc.queryForObject(
                    "SELECT count(*) FROM complaints WHERE booking_id = ?",
                    Integer.class,
                    bookingId))
            .isEqualTo(1);
      } finally {
        pool.shutdownNow();
      }
    }

    @Test
    @DisplayName("Loại sai, nội dung rỗng hoặc OTHER quá ngắn trả 400 tiếng Việt")
    void shouldReturnBadRequestWhenBodyInvalid() throws Exception {
      UUID bookingId = insertBooking(CUSTOMER_ID, "CHECKED_OUT");

      submit(bookingId, customer(CUSTOMER_ID), "{\"category\": \"NOISE\", \"content\": \"Ồn\"}")
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].field").value("category"));
      submit(bookingId, customer(CUSTOMER_ID), "{\"category\": \"BILLING\", \"content\": \"  \"}")
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].field").value("content"));
      submit(bookingId, customer(CUSTOMER_ID), "{\"category\": \"OTHER\", \"content\": \"Ngắn\"}")
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
          .andExpect(
              jsonPath("$.detail").value("Khiếu nại loại OTHER cần mô tả tối thiểu 20 ký tự."));
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM complaints WHERE booking_id = ?", Integer.class, bookingId))
          .isZero();
    }
  }

  @Nested
  @DisplayName("GET /api/v1/me/complaints và /{complaintId}")
  class ReadApiTests {

    @Test
    @DisplayName("Chỉ thấy khiếu nại của mình, lọc theo trạng thái; chi tiết của người khác 404")
    void shouldListAndShowOnlyOwnComplaints() throws Exception {
      // Given
      String mine =
          complaintIdFrom(
              submit(insertBooking(CUSTOMER_ID, "CHECKED_OUT"), customer(CUSTOMER_ID), VALID_BODY)
                  .andExpect(status().isCreated()));
      String others =
          complaintIdFrom(
              submit(
                      insertBooking(OTHER_CUSTOMER_ID, "CHECKED_OUT"),
                      customer(OTHER_CUSTOMER_ID),
                      VALID_BODY)
                  .andExpect(status().isCreated()));

      // When / Then
      mvc.perform(get("/api/v1/me/complaints").with(customer(CUSTOMER_ID)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("Lấy danh sách khiếu nại thành công."))
          .andExpect(jsonPath("$.data.content", hasSize(1)))
          .andExpect(jsonPath("$.data.content[0].id").value(mine))
          .andExpect(jsonPath("$.data.totalElements").value(1));
      mvc.perform(
              get("/api/v1/me/complaints").param("status", "RESOLVED").with(customer(CUSTOMER_ID)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content", hasSize(0)));
      mvc.perform(get("/api/v1/me/complaints").param("status", "DONE").with(customer(CUSTOMER_ID)))
          .andExpect(status().isBadRequest());
      mvc.perform(get("/api/v1/me/complaints/" + mine).with(customer(CUSTOMER_ID)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.id").value(mine))
          .andExpect(jsonPath("$.data.content").value("Phòng có mùi ẩm mốc."));
      mvc.perform(get("/api/v1/me/complaints/" + others).with(customer(CUSTOMER_ID)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("COMPLAINT_NOT_FOUND"));
      mvc.perform(get("/api/v1/me/complaints")).andExpect(status().isUnauthorized());
    }
  }
}
