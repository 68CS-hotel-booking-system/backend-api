package com.team.hotelbooking;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.team.hotelbooking.booking.complaint.application.command.HandleComplaintCommand;
import com.team.hotelbooking.booking.complaint.application.usecase.HandleComplaintUseCase;
import com.team.hotelbooking.booking.complaint.domain.exception.ComplaintException;
import com.team.hotelbooking.booking.complaint.domain.model.Complaint;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
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

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Xử lý khiếu nại: phạm vi, lịch sử, cạnh tranh cập nhật và rollback")
class ComplaintHandlingIntegrationTests {
  private static final UUID OWNER = UUID.fromString("8d000000-0000-4000-8000-000000000001");
  private static final UUID OTHER_OWNER = UUID.fromString("8d000000-0000-4000-8000-000000000002");
  private static final UUID ADMIN = UUID.fromString("8d000000-0000-4000-8000-000000000003");
  private static final UUID CUSTOMER = UUID.fromString("8d000000-0000-4000-8000-000000000004");
  private static final UUID STAFF = UUID.fromString("8d000000-0000-4000-8000-000000000005");
  private static final UUID HOTEL = UUID.fromString("8d000000-0000-4000-8000-000000000101");
  private static final UUID OTHER_HOTEL = UUID.fromString("8d000000-0000-4000-8000-000000000102");
  private static final UUID CITY = UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3");
  private static final String BASE = "/api/v1/management/complaints";
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired HandleComplaintUseCase handler;

  @BeforeEach
  void setUp() {
    user(OWNER, "HOTEL_OWNER", 1);
    user(OTHER_OWNER, "HOTEL_OWNER", 2);
    user(ADMIN, "ADMIN", 3);
    user(CUSTOMER, "CUSTOMER", 4);
    user(STAFF, "CUSTOMER", 5);
    hotel(HOTEL, OWNER);
    hotel(OTHER_HOTEL, OTHER_OWNER);
    clearComplaints();
  }

  // Không để fixture khiếu nại/lịch sử ảnh hưởng các nhóm test dùng chung PostgreSQL.
  @AfterEach
  void clearComplaints() {
    jdbc.update(
        "DELETE FROM complaint_actions WHERE complaint_id IN (SELECT id FROM complaints WHERE hotel_id IN (?,?))",
        HOTEL,
        OTHER_HOTEL);
    jdbc.update("DELETE FROM complaints WHERE hotel_id IN (?,?)", HOTEL, OTHER_HOTEL);
  }

  @Test
  @DisplayName("Owner chỉ thấy khách sạn của mình; admin thấy cả hai")
  void shouldScopeListDetailsAndActions() throws Exception {
    UUID mine = complaint(HOTEL, "Phòng bị ẩm");
    UUID other = complaint(OTHER_HOTEL, "Khách sạn khác");
    mvc.perform(get(BASE).with(actor(OWNER, "HOTEL_OWNER")))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].complaint.id").value(mine.toString()));
    mvc.perform(get(BASE).with(actor(ADMIN, "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2));
    mvc.perform(get(BASE + "/" + other).with(actor(OWNER, "HOTEL_OWNER")))
        .andExpect(status().isNotFound());
    mvc.perform(
            get(BASE).param("hotelId", OTHER_HOTEL.toString()).with(actor(OWNER, "HOTEL_OWNER")))
        .andExpect(status().isNotFound());
    action(other, OWNER, "HOTEL_OWNER", "IN_REVIEW", 0, "Đang kiểm tra")
        .andExpect(status().isNotFound());
    assertThat(historyCount(other)).isZero();
  }

  @Test
  @DisplayName("Phản hồi nhiều lần, đóng khiếu nại và hiển thị lịch sử cho đúng khách")
  void shouldPersistLifecycleAndCustomerVisibleHistory() throws Exception {
    UUID id = complaint(HOTEL, "Phòng có mùi ẩm");
    action(id, OWNER, "HOTEL_OWNER", "IN_REVIEW", 0, " Đã tiếp nhận kiểm tra. ")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.complaint.version").value(1))
        .andExpect(
            jsonPath("$.data.complaint.history[0].response").value("Đã tiếp nhận kiểm tra."));
    action(id, ADMIN, "ADMIN", "IN_REVIEW", 1, "Nền tảng đang phối hợp.")
        .andExpect(status().isOk());
    action(id, OWNER, "HOTEL_OWNER", "RESOLVED", 2, "Đã đổi phòng cho khách.")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.complaint.status").value("RESOLVED"))
        .andExpect(jsonPath("$.data.complaint.history.length()").value(3));
    mvc.perform(get("/api/v1/me/complaints/" + id).with(actor(CUSTOMER, "CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.history[1].actorRole").value("ADMIN"))
        .andExpect(jsonPath("$.data.history[2].response").value("Đã đổi phòng cho khách."))
        .andExpect(jsonPath("$.data.history[0].actorUserId").doesNotExist());
    mvc.perform(get("/api/v1/me/complaints/" + id).with(actor(STAFF, "CUSTOMER")))
        .andExpect(status().isNotFound());
    action(id, ADMIN, "ADMIN", "REJECTED", 3, "Đổi kết quả")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_COMPLAINT_TRANSITION"));
    assertThat(
            jdbc.queryForList(
                "SELECT actor_user_id FROM complaint_actions WHERE complaint_id=? ORDER BY version",
                UUID.class,
                id))
        .containsExactly(OWNER, ADMIN, OWNER);
  }

  @Test
  @DisplayName("Không bỏ qua bước xử lý, không nhận phản hồi rỗng và không ghi đè phiên bản cũ")
  void shouldValidateTransitionsAndVersions() throws Exception {
    UUID id = complaint(HOTEL, "Nội dung");
    action(id, OWNER, "HOTEL_OWNER", "RESOLVED", 0, "Chưa kiểm tra")
        .andExpect(status().isConflict());
    action(id, OWNER, "HOTEL_OWNER", "IN_REVIEW", 0, "   ").andExpect(status().isBadRequest());
    action(id, OWNER, "HOTEL_OWNER", "IN_REVIEW", -1, "Kiểm tra")
        .andExpect(status().isBadRequest());
    action(id, OWNER, "HOTEL_OWNER", "IN_REVIEW", 0, "Đã tiếp nhận").andExpect(status().isOk());
    action(id, ADMIN, "ADMIN", "REJECTED", 0, "Kết quả cũ")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("COMPLAINT_VERSION_CONFLICT"));
    action(id, ADMIN, "ADMIN", "REJECTED", 1, "Không đủ căn cứ.").andExpect(status().isOk());
    assertThat(historyCount(id)).isEqualTo(2);
  }

  @Test
  @DisplayName("Kiểm role trong DB, trạng thái và xác minh tài khoản thay vì tin JWT cũ")
  void shouldRejectCustomersStaffAndStaleRoles() throws Exception {
    UUID id = complaint(HOTEL, "Nội dung");
    mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    mvc.perform(get(BASE).with(actor(CUSTOMER, "CUSTOMER"))).andExpect(status().isForbidden());
    mvc.perform(get(BASE).with(actor(STAFF, "HOTEL_OWNER"))).andExpect(status().isForbidden());
    action(id, CUSTOMER, "ADMIN", "IN_REVIEW", 0, "Giả role").andExpect(status().isForbidden());
    jdbc.update("UPDATE users SET status='inactive' WHERE id=?", OWNER);
    mvc.perform(get(BASE).with(actor(OWNER, "HOTEL_OWNER"))).andExpect(status().isForbidden());
    jdbc.update("UPDATE users SET status='active',email_verified_at=NULL WHERE id=?", OWNER);
    action(id, OWNER, "HOTEL_OWNER", "IN_REVIEW", 0, "Chưa xác minh")
        .andExpect(status().isForbidden());
    assertThat(historyCount(id)).isZero();
  }

  @Test
  @DisplayName("Khách sạn đổi chủ thì quyền quản lý khiếu nại chuyển theo khách sạn")
  void shouldFollowCurrentHotelOwnership() throws Exception {
    UUID id = complaint(HOTEL, "Nội dung");
    jdbc.update("UPDATE hotels SET owner_user_id=? WHERE id=?", OTHER_OWNER, HOTEL);
    mvc.perform(get(BASE + "/" + id).with(actor(OWNER, "HOTEL_OWNER")))
        .andExpect(status().isNotFound());
    action(id, OTHER_OWNER, "HOTEL_OWNER", "IN_REVIEW", 0, "Chủ mới tiếp nhận")
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Tìm kiếm ký tự đặc biệt đúng nghĩa và phân trang trong phạm vi owner")
  void shouldSearchAndPaginateWithoutWildcardLeakage() throws Exception {
    complaint(HOTEL, "room_% có mùi");
    complaint(HOTEL, "roomAB bình thường");
    complaint(OTHER_HOTEL, "room_% ngoài phạm vi");
    mvc.perform(get(BASE).param("keyword", "room_%").with(actor(OWNER, "HOTEL_OWNER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));
    mvc.perform(get(BASE).param("size", "1").param("page", "1").with(actor(OWNER, "HOTEL_OWNER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$.data.content.length()").value(1));
    mvc.perform(get(BASE).param("status", "RESOLVED").with(actor(OWNER, "HOTEL_OWNER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(0));
    mvc.perform(get(BASE).param("status", "FAKE").with(actor(OWNER, "HOTEL_OWNER")))
        .andExpect(status().isBadRequest());
    mvc.perform(get(BASE).param("page", "-1").with(actor(OWNER, "HOTEL_OWNER")))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Hai người xử lý cùng phiên bản: chỉ một người thành công và một lịch sử được ghi")
  void shouldSerializeCompetingActionsByVersion() throws Exception {
    UUID id = complaint(HOTEL, "Nội dung");
    var barrier = new CyclicBarrier(2);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var futures = new ArrayList<Future<String>>();
      for (UUID actorId : List.of(OWNER, ADMIN))
        futures.add(
            pool.submit(
                () -> {
                  barrier.await(10, TimeUnit.SECONDS);
                  try {
                    handler.execute(
                        new HandleComplaintCommand(
                            actorId, id, Complaint.Status.IN_REVIEW, "Tiếp nhận", 0L));
                    return "OK";
                  } catch (ComplaintException error) {
                    return error.code().name();
                  }
                }));
      var results = new ArrayList<String>();
      for (var future : futures) results.add(future.get(30, TimeUnit.SECONDS));
      assertThat(results).containsExactlyInAnyOrder("OK", "COMPLAINT_VERSION_CONFLICT");
    }
    assertThat(historyCount(id)).isEqualTo(1);
  }

  @Test
  @DisplayName("Ghi lịch sử lỗi thì trạng thái và phiên bản cũng rollback")
  void shouldRollbackWhenHistoryInsertFails() {
    UUID id = complaint(HOTEL, "Nội dung");
    jdbc.execute(
        "CREATE FUNCTION fail_complaint_action_it() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.complaint_id = '"
            + id
            + "'::uuid THEN RAISE EXCEPTION 'test history failure'; END IF; RETURN NEW; END $$");
    jdbc.execute(
        "CREATE TRIGGER fail_complaint_action_it BEFORE INSERT ON complaint_actions FOR EACH ROW EXECUTE FUNCTION fail_complaint_action_it()");
    try {
      assertThatThrownBy(
              () ->
                  handler.execute(
                      new HandleComplaintCommand(
                          OWNER, id, Complaint.Status.IN_REVIEW, "Tiếp nhận", 0L)))
          .isInstanceOf(org.springframework.dao.DataAccessException.class);
      assertThat(
              jdbc.queryForObject(
                  "SELECT status::text FROM complaints WHERE id=?", String.class, id))
          .isEqualTo("OPEN");
      assertThat(jdbc.queryForObject("SELECT version FROM complaints WHERE id=?", Long.class, id))
          .isZero();
      assertThat(historyCount(id)).isZero();
    } finally {
      jdbc.execute("DROP TRIGGER fail_complaint_action_it ON complaint_actions");
      jdbc.execute("DROP FUNCTION fail_complaint_action_it()");
    }
  }

  private long historyCount(UUID id) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM complaint_actions WHERE complaint_id=?", Long.class, id);
  }

  private ResultActions action(
      UUID id, UUID user, String role, String next, long version, String response)
      throws Exception {
    return mvc.perform(
        post(BASE + "/" + id + "/actions")
            .with(actor(user, role))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"status\":\""
                    + next
                    + "\",\"response\":\""
                    + response
                    + "\",\"expectedVersion\":"
                    + version
                    + "}"));
  }

  private static JwtRequestPostProcessor actor(UUID id, String role) {
    return jwt()
        .jwt(token -> token.subject(id.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  private void user(UUID id, String role, int suffix) {
    jdbc.update(
        "INSERT INTO users(id,email,password_hash,full_name,phone,status,email_verified_at) VALUES(?,?,'not-real','Xử lý khiếu nại',?,'active',now()) ON CONFLICT(id) DO UPDATE SET status='active',email_verified_at=now()",
        id,
        "handling-" + id + "@test.local",
        "+8495900000" + suffix);
    jdbc.update(
        "INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE role_key=? ON CONFLICT DO NOTHING",
        id,
        role);
  }

  private void hotel(UUID id, UUID owner) {
    jdbc.update(
        "INSERT INTO hotels(id,owner_user_id,city_id,name,slug,address,status) VALUES(?,?,?,'Khách sạn xử lý',?,'Test','ACTIVE') ON CONFLICT(id) DO UPDATE SET owner_user_id=EXCLUDED.owner_user_id",
        id,
        owner,
        CITY,
        "handling-" + id);
  }

  private UUID complaint(UUID hotel, String content) {
    UUID booking = UUID.randomUUID();
    UUID id = UUID.randomUUID();
    jdbc.update(
        """
      INSERT INTO bookings(id,booking_code,customer_id,hotel_id,booker_name_snapshot,booker_email_snapshot,booker_phone_snapshot,
        guest_name_snapshot,guest_email_snapshot,guest_phone_snapshot,check_in,check_out,days,nights,status)
      VALUES(?,?,?,?,'Người đặt thử','booker@test.local','0900000000','Khách','guest@test.local','0900000001','2027-06-01','2027-06-03',3,2,'CONFIRMED')
      """,
        booking,
        "HND" + booking.toString().substring(0, 8),
        CUSTOMER,
        hotel);
    jdbc.update(
        "INSERT INTO complaints(id,booking_id,customer_id,hotel_id,category,content,status,created_at,updated_at,version) VALUES(?,?,?,?,'ROOM_CONDITION',?,'OPEN',now(),now(),0)",
        id,
        booking,
        CUSTOMER,
        hotel,
        content);
    return id;
  }
}
