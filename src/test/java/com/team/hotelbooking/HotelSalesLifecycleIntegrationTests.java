package com.team.hotelbooking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.ObjectMapper;

// Đặt ở package gốc vì TestcontainersConfiguration là package-private.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Vòng đời bán khách sạn trên PostgreSQL (HT-03, HT-05, HT-06, HT-08, HT-09)")
class HotelSalesLifecycleIntegrationTests {

  private static final UUID ADMIN_ID = UUID.fromString("aaaa1111-0000-4000-8000-000000000801");
  private static final UUID OWNER_ID = UUID.fromString("aaaa1111-0000-4000-8000-000000000802");
  private static final UUID MANAGER_ID = UUID.fromString("aaaa1111-0000-4000-8000-000000000803");
  private static final UUID RECEPTIONIST_ID =
      UUID.fromString("aaaa1111-0000-4000-8000-000000000804");
  private static final UUID CUSTOMER_ID = UUID.fromString("aaaa1111-0000-4000-8000-000000000805");
  // Đủ điều kiện HT-03: có liên hệ, loại phòng hợp lệ đang bán và phòng đang phục vụ.
  private static final UUID READY_HOTEL_ID =
      UUID.fromString("bbbb2222-0000-4000-8000-000000000801");
  // Thiếu liên hệ, loại phòng duy nhất đã ngừng bán, chưa có phòng vật lý.
  private static final UUID INCOMPLETE_HOTEL_ID =
      UUID.fromString("bbbb2222-0000-4000-8000-000000000802");
  private static final UUID READY_ROOM_TYPE_ID =
      UUID.fromString("cccc3333-0000-4000-8000-000000000801");
  private static final UUID INCOMPLETE_ROOM_TYPE_ID =
      UUID.fromString("cccc3333-0000-4000-8000-000000000802");
  private static final UUID ROOM_ID = UUID.fromString("dddd4444-0000-4000-8000-000000000801");
  private static final UUID DA_NANG_ID = UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3");

  @Autowired private MockMvc mvc;

  @Autowired private JdbcTemplate jdbc;

  @Autowired private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    // Fixture riêng; mỗi test đưa khách sạn về DRAFT và xóa lịch sử của fixture vì DB dùng chung.
    insertUser(ADMIN_ID, "ADMIN");
    insertUser(OWNER_ID, "HOTEL_OWNER");
    insertUser(MANAGER_ID, "CUSTOMER");
    insertUser(RECEPTIONIST_ID, "CUSTOMER");
    insertUser(CUSTOMER_ID, "CUSTOMER");
    resetHotel(READY_HOTEL_ID, "+84905123456");
    resetHotel(INCOMPLETE_HOTEL_ID, null);
    insertRoomType(READY_ROOM_TYPE_ID, READY_HOTEL_ID, true);
    insertRoomType(INCOMPLETE_ROOM_TYPE_ID, INCOMPLETE_HOTEL_ID, false);
    jdbc.update(
        """
        INSERT INTO rooms (id, hotel_id, room_type_id, room_number, floor, operational_from)
        VALUES (?, ?, ?, '101', '1', '2026-01-01T00:00:00Z')
        ON CONFLICT (id) DO NOTHING
        """,
        ROOM_ID,
        READY_HOTEL_ID,
        READY_ROOM_TYPE_ID);
    insertMember(READY_HOTEL_ID, MANAGER_ID, "HOTEL_MANAGER");
    insertMember(READY_HOTEL_ID, RECEPTIONIST_ID, "RECEPTIONIST");
  }

  @Nested
  @DisplayName("Migration trạng thái và liên hệ")
  class MigrationTests {

    @Test
    void shouldKeepOldValuesAndAddSuspendedAndLockedToHotelStatusEnum() {
      assertEquals(
          "{DRAFT,ACTIVE,SUSPENDED,LOCKED}",
          jdbc.queryForObject("SELECT enum_range(NULL::hotel_status__t)::text", String.class));
    }

    @Test
    void shouldRejectHistoryRowWhenFromAndToStatusAreEqual() {
      assertThrows(
          DataIntegrityViolationException.class,
          () ->
              jdbc.update(
                  """
                  INSERT INTO hotel_status_history
                    (hotel_id, from_status, to_status, changed_by_user_id, changed_at)
                  VALUES (?, 'ACTIVE', 'ACTIVE', ?, now())
                  """,
                  READY_HOTEL_ID,
                  ADMIN_ID));
    }
  }

  @Nested
  @DisplayName("PATCH /api/v1/management/hotels/{hotelId}/approve (HT-03)")
  class ApproveApiTests {

    @Test
    void shouldReturnUnprocessableWithEveryReasonAndKeepDraftWhenHotelIsIncomplete()
        throws Exception {
      // When
      approve(INCOMPLETE_HOTEL_ID, admin())
          .andExpect(status().is(422))
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_READY"))
          .andExpect(jsonPath("$.errors[0].field").value("contact"))
          .andExpect(jsonPath("$.errors[1].field").value("roomTypes"));

      // Then
      assertEquals("DRAFT", statusOf(INCOMPLETE_HOTEL_ID));
      assertEquals(0, historyOf(INCOMPLETE_HOTEL_ID).size());
    }

    @Test
    void shouldReturnUnprocessableWhenRoomTypeHasNoServingRoom() throws Exception {
      // Given
      jdbc.update("UPDATE rooms SET operational_until = now() WHERE id = ?", ROOM_ID);

      // When / Then
      approve(READY_HOTEL_ID, admin())
          .andExpect(status().is(422))
          .andExpect(jsonPath("$.errors.length()").value(1))
          .andExpect(jsonPath("$.errors[0].field").value("rooms"));
      assertEquals("DRAFT", statusOf(READY_HOTEL_ID));
    }

    @Test
    void shouldApproveRecordHistoryAndShowHotelPubliclyWhenHotelIsReady() throws Exception {
      // Given
      getPublicHotel(READY_HOTEL_ID).andExpect(status().isNotFound());

      // When
      approve(READY_HOTEL_ID, admin())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("Duyệt khách sạn thành công."))
          .andExpect(jsonPath("$.data.status").value("ACTIVE"));

      // Then
      assertEquals("ACTIVE", statusOf(READY_HOTEL_ID));
      List<Map<String, Object>> history = historyOf(READY_HOTEL_ID);
      assertEquals(1, history.size());
      assertEquals("DRAFT", history.get(0).get("from_status"));
      assertEquals("ACTIVE", history.get(0).get("to_status"));
      assertNull(history.get(0).get("reason"));
      assertEquals(ADMIN_ID, history.get(0).get("changed_by_user_id"));
      getPublicHotel(READY_HOTEL_ID).andExpect(status().isOk());
      getAvailability(READY_HOTEL_ID).andExpect(status().isOk());
    }

    @Test
    void shouldReturnBadRequestWithoutHistoryWhenHotelIsAlreadyActive() throws Exception {
      // Given
      jdbc.update("UPDATE hotels SET status = 'ACTIVE' WHERE id = ?", READY_HOTEL_ID);

      // When / Then
      approve(READY_HOTEL_ID, admin())
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
      assertEquals(0, historyOf(READY_HOTEL_ID).size());
    }

    @Test
    void shouldForbidOwnerFromApprovingFirstTime() throws Exception {
      // When / Then
      approve(READY_HOTEL_ID, owner()).andExpect(status().isForbidden());
      assertEquals("DRAFT", statusOf(READY_HOTEL_ID));
    }

    @Test
    void shouldReturnConflictWhenManagerResumesHotelNeverApproved() throws Exception {
      // When / Then
      post("/api/v1/management/hotels/" + READY_HOTEL_ID + "/resume", null, manager())
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
      assertEquals("DRAFT", statusOf(READY_HOTEL_ID));
    }

    @Test
    void shouldReturnNotFoundForPublishEndpointRemovedInFavourOfApprove() throws Exception {
      // When / Then
      post("/api/v1/admin/hotels/" + READY_HOTEL_ID + "/publish", null, admin())
          .andExpect(status().isNotFound());
      assertEquals("DRAFT", statusOf(READY_HOTEL_ID));
    }
  }

  @Nested
  @DisplayName("POST /api/v1/management/hotels/{hotelId}/suspend và /resume")
  class SuspendResumeApiTests {

    @Test
    void shouldHideHotelFromPublicAndBookingSearchWhileSuspendedThenShowAfterResume()
        throws Exception {
      // Given
      jdbc.update("UPDATE hotels SET status = 'ACTIVE' WHERE id = ?", READY_HOTEL_ID);

      // When
      post(
              "/api/v1/management/hotels/" + READY_HOTEL_ID + "/suspend",
              "{\"reason\": \"Sửa hồ bơi\"}",
              manager())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

      // Then
      getPublicHotel(READY_HOTEL_ID).andExpect(status().isNotFound());
      getAvailability(READY_HOTEL_ID)
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));

      // When
      post("/api/v1/management/hotels/" + READY_HOTEL_ID + "/resume", null, manager())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("ACTIVE"));

      // Then
      getPublicHotel(READY_HOTEL_ID).andExpect(status().isOk());
      List<Map<String, Object>> history = historyOf(READY_HOTEL_ID);
      assertEquals(2, history.size());
      assertEquals("SUSPENDED", history.get(0).get("to_status"));
      assertEquals("Sửa hồ bơi", history.get(0).get("reason"));
      assertEquals(MANAGER_ID, history.get(0).get("changed_by_user_id"));
      assertNull(history.get(1).get("reason"));
    }

    @Test
    void shouldReturnUnprocessableOnResumeWhenLastRoomStoppedServingDuringSuspension()
        throws Exception {
      // Given
      jdbc.update("UPDATE hotels SET status = 'SUSPENDED' WHERE id = ?", READY_HOTEL_ID);
      jdbc.update("UPDATE rooms SET operational_until = now() WHERE id = ?", ROOM_ID);

      // When / Then
      post("/api/v1/management/hotels/" + READY_HOTEL_ID + "/resume", null, owner())
          .andExpect(status().is(422))
          .andExpect(jsonPath("$.errors[0].field").value("rooms"));
      assertEquals("SUSPENDED", statusOf(READY_HOTEL_ID));
    }

    @Test
    void shouldReturnForbiddenForReceptionistAndNotFoundForCustomer() throws Exception {
      // Given
      jdbc.update("UPDATE hotels SET status = 'ACTIVE' WHERE id = ?", READY_HOTEL_ID);

      // When / Then
      post("/api/v1/management/hotels/" + READY_HOTEL_ID + "/suspend", null, receptionist())
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
      post("/api/v1/management/hotels/" + READY_HOTEL_ID + "/suspend", null, customer())
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
      assertEquals("ACTIVE", statusOf(READY_HOTEL_ID));
    }
  }

  @Nested
  @DisplayName("POST /api/v1/admin/hotels/{hotelId}/lock và /unlock")
  class LockApiTests {

    @Test
    void shouldBlockManagerUntilAdminUnlocksThenReturnToSuspended() throws Exception {
      // Given
      jdbc.update("UPDATE hotels SET status = 'ACTIVE' WHERE id = ?", READY_HOTEL_ID);

      // When
      post(
              "/api/v1/admin/hotels/" + READY_HOTEL_ID + "/lock",
              "{\"reason\": \"Vi phạm quy định\"}",
              admin())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("LOCKED"));

      // Then
      getPublicHotel(READY_HOTEL_ID).andExpect(status().isNotFound());
      post("/api/v1/management/hotels/" + READY_HOTEL_ID + "/resume", null, owner())
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("HOTEL_LOCKED"));
      post("/api/v1/management/hotels/" + READY_HOTEL_ID + "/suspend", null, manager())
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("HOTEL_LOCKED"));
      post("/api/v1/admin/hotels/" + READY_HOTEL_ID + "/unlock", "{\"reason\": \"Gỡ\"}", owner())
          .andExpect(status().isForbidden());

      // When
      post(
              "/api/v1/admin/hotels/" + READY_HOTEL_ID + "/unlock",
              "{\"reason\": \"Đã xử lý vi phạm\"}",
              admin())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

      // Then
      List<Map<String, Object>> history = historyOf(READY_HOTEL_ID);
      assertEquals(2, history.size());
      assertEquals("Vi phạm quy định", history.get(0).get("reason"));
      assertEquals("LOCKED", history.get(1).get("from_status"));
      assertEquals("SUSPENDED", history.get(1).get("to_status"));
      post("/api/v1/management/hotels/" + READY_HOTEL_ID + "/resume", null, owner())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void shouldReturnHotelLockedFromDraftBackToDraftWhenUnlocking() throws Exception {
      // When
      post(
              "/api/v1/admin/hotels/" + READY_HOTEL_ID + "/lock",
              "{\"reason\": \"Nghi ngờ giả mạo\"}",
              admin())
          .andExpect(status().isOk());
      post(
              "/api/v1/admin/hotels/" + READY_HOTEL_ID + "/unlock",
              "{\"reason\": \"Đã xác minh\"}",
              admin())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("DRAFT"));

      // Then
      assertEquals("DRAFT", statusOf(READY_HOTEL_ID));
    }

    @Test
    void shouldReturnBadRequestAndKeepStatusWhenLockReasonIsMissing() throws Exception {
      // When / Then
      post("/api/v1/admin/hotels/" + READY_HOTEL_ID + "/lock", "{}", admin())
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].field").value("reason"));
      assertEquals("DRAFT", statusOf(READY_HOTEL_ID));
      assertEquals(0, historyOf(READY_HOTEL_ID).size());
    }
  }

  @Nested
  @DisplayName("PATCH liên hệ khách sạn (HT-03)")
  class ContactApiTests {

    @Test
    void shouldPersistContactsWithoutExposingThemOnPublicApi() throws Exception {
      // Given
      long version =
          jdbc.queryForObject(
              "SELECT version FROM hotels WHERE id = ?", Long.class, INCOMPLETE_HOTEL_ID);

      // When
      mvc.perform(
              patch("/api/v1/management/hotels/" + INCOMPLETE_HOTEL_ID)
                  .with(owner())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      "{\"contactPhone\": \" +84905000111 \", \"contactEmail\":"
                          + " \"lienhe@test.local\", \"version\": "
                          + version
                          + "}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.contactPhone").value("+84905000111"))
          .andExpect(jsonPath("$.data.contactEmail").value("lienhe@test.local"));

      // Then
      Map<String, Object> row =
          jdbc.queryForMap(
              "SELECT contact_phone, contact_email FROM hotels WHERE id = ?", INCOMPLETE_HOTEL_ID);
      assertEquals("+84905000111", row.get("contact_phone"));
      assertEquals("lienhe@test.local", row.get("contact_email"));

      jdbc.update("UPDATE hotels SET status = 'ACTIVE' WHERE id = ?", INCOMPLETE_HOTEL_ID);
      String body =
          getPublicHotel(INCOMPLETE_HOTEL_ID)
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();
      var data = mapper.readTree(body).get("data");
      assertFalse(data.has("contactPhone"), "API công khai không trả số điện thoại liên hệ");
      assertFalse(data.has("contactEmail"), "API công khai không trả email liên hệ");
    }
  }

  private ResultActions post(String uri, String body, JwtRequestPostProcessor actor)
      throws Exception {
    var request = MockMvcRequestBuilders.post(uri);
    request.with(actor);
    if (body != null) {
      request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
    return mvc.perform(request);
  }

  private ResultActions approve(UUID hotelId, JwtRequestPostProcessor actor) throws Exception {
    return mvc.perform(patch("/api/v1/management/hotels/" + hotelId + "/approve").with(actor));
  }

  private ResultActions getPublicHotel(UUID hotelId) throws Exception {
    return mvc.perform(get("/api/v1/hotels/" + hotelId));
  }

  private ResultActions getAvailability(UUID hotelId) throws Exception {
    return mvc.perform(
        get("/api/v1/hotels/" + hotelId + "/availability")
            .param("checkInDate", "2027-06-01")
            .param("checkOutDate", "2027-06-03"));
  }

  private String statusOf(UUID hotelId) {
    return jdbc.queryForObject(
        "SELECT status::text FROM hotels WHERE id = ?", String.class, hotelId);
  }

  private List<Map<String, Object>> historyOf(UUID hotelId) {
    return jdbc.queryForList(
        """
        SELECT from_status::text AS from_status, to_status::text AS to_status, reason,
               changed_by_user_id
        FROM hotel_status_history WHERE hotel_id = ? ORDER BY changed_at, id
        """,
        hotelId);
  }

  private static JwtRequestPostProcessor actor(UUID userId, String role) {
    return jwt()
        .jwt(token -> token.subject(userId.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  private static JwtRequestPostProcessor admin() {
    return actor(ADMIN_ID, "ADMIN");
  }

  private static JwtRequestPostProcessor owner() {
    return actor(OWNER_ID, "HOTEL_OWNER");
  }

  private static JwtRequestPostProcessor manager() {
    return actor(MANAGER_ID, "CUSTOMER");
  }

  private static JwtRequestPostProcessor receptionist() {
    return actor(RECEPTIONIST_ID, "CUSTOMER");
  }

  private static JwtRequestPostProcessor customer() {
    return actor(CUSTOMER_ID, "CUSTOMER");
  }

  private void insertUser(UUID id, String roleKey) {
    jdbc.update(
        """
        INSERT INTO users (id, email, password_hash, full_name, phone, status, email_verified_at)
        VALUES (?, ?, 'not-a-real-hash', 'Người dùng test', ?, 'active', now())
        ON CONFLICT (id) DO NOTHING
        """,
        id,
        "sales-lifecycle-" + id + "@test.local",
        // phone UNIQUE: dải +84966... riêng của lớp này, lấy 6 số cuối của id.
        "+84966" + id.toString().substring(30));
    jdbc.update(
        """
        INSERT INTO user_roles (user_id, role_id)
        SELECT ?, id FROM roles WHERE role_key = ?
        ON CONFLICT (user_id, role_id) DO NOTHING
        """,
        id,
        roleKey);
  }

  private void resetHotel(UUID id, String contactPhone) {
    jdbc.update("DELETE FROM hotel_status_history WHERE hotel_id = ?", id);
    jdbc.update(
        """
        INSERT INTO hotels (id, owner_user_id, city_id, name, slug, address, contact_phone)
        VALUES (?, ?, ?, 'Khách sạn vòng đời', ?, '3 Nguyễn Văn Linh', ?)
        ON CONFLICT (id) DO UPDATE SET
          status = 'DRAFT', contact_phone = EXCLUDED.contact_phone, contact_email = NULL
        """,
        id,
        OWNER_ID,
        DA_NANG_ID,
        "sales-lifecycle-" + id,
        contactPhone);
  }

  private void insertRoomType(UUID id, UUID hotelId, boolean active) {
    jdbc.update(
        """
        INSERT INTO room_types (id, hotel_id, name, max_adults, max_children, max_occupancy,
                                is_active)
        VALUES (?, ?, 'Phòng vòng đời', 2, 0, 2, ?)
        ON CONFLICT (id) DO UPDATE SET is_active = EXCLUDED.is_active
        """,
        id,
        hotelId,
        active);
    if (id.equals(READY_ROOM_TYPE_ID)) {
      jdbc.update("UPDATE rooms SET operational_until = NULL WHERE id = ?", ROOM_ID);
    }
  }

  private void insertMember(UUID hotelId, UUID userId, String role) {
    jdbc.update(
        """
        INSERT INTO hotel_members (hotel_id, user_id, role)
        VALUES (?, ?, CAST(? AS hotel_member_role__t))
        ON CONFLICT (hotel_id, user_id, role) DO UPDATE SET is_active = true
        """,
        hotelId,
        userId,
        role);
  }
}
