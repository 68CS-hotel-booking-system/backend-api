package com.team.hotelbooking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

// Đặt ở package gốc vì TestcontainersConfiguration là package-private.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Sửa loại phòng (HT17) và phòng vật lý (HT21) trên PostgreSQL")
class ManagementRoomUpdateIntegrationTests {

  private static final UUID OWNER_ID = UUID.fromString("55555555-eeee-4eee-8eee-000000000501");
  private static final UUID HOTEL_ID = UUID.fromString("66666666-ffff-4fff-8fff-000000000501");
  private static final UUID OTHER_HOTEL_ID =
      UUID.fromString("66666666-ffff-4fff-8fff-000000000502");
  private static final UUID ROOM_TYPE_ID = UUID.fromString("77777777-aaaa-4aaa-8aaa-000000000501");
  private static final UUID ROOM_ID = UUID.fromString("88888888-bbbb-4bbb-8bbb-000000000501");
  private static final UUID SECOND_ROOM_ID =
      UUID.fromString("88888888-bbbb-4bbb-8bbb-000000000502");
  private static final UUID DA_NANG_ID = UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3");
  private static final UUID ADMIN_ID = UUID.fromString("55555555-eeee-4eee-8eee-000000000502");
  private static final UUID MANAGER_ID = UUID.fromString("55555555-eeee-4eee-8eee-000000000503");
  private static final UUID OTHER_MANAGER_ID =
      UUID.fromString("55555555-eeee-4eee-8eee-000000000504");
  private static final UUID RECEPTIONIST_ID =
      UUID.fromString("55555555-eeee-4eee-8eee-000000000505");
  private static final UUID CUSTOMER_ID = UUID.fromString("55555555-eeee-4eee-8eee-000000000506");
  private static final UUID MISSING_HOTEL_ID =
      UUID.fromString("66666666-ffff-4fff-8fff-000000000599");
  private static final Instant BASELINE = Instant.parse("2026-10-01T00:00:00Z");
  private static final String ROOM_TYPE_PATH =
      "/api/v1/management/hotels/" + HOTEL_ID + "/room-types/" + ROOM_TYPE_ID;
  private static final String ROOM_PATH =
      "/api/v1/management/hotels/" + HOTEL_ID + "/rooms/" + ROOM_ID;

  @Autowired private MockMvc mvc;

  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void setUp() {
    // Fixture riêng của lớp này; mỗi test đưa dòng về trạng thái gốc vì DB dùng chung cả suite.
    insertOwner();
    insertUser(ADMIN_ID, "ADMIN");
    insertUser(MANAGER_ID, "CUSTOMER");
    insertUser(OTHER_MANAGER_ID, "CUSTOMER");
    insertUser(RECEPTIONIST_ID, "CUSTOMER");
    insertUser(CUSTOMER_ID, "CUSTOMER");
    insertHotel(HOTEL_ID);
    insertHotel(OTHER_HOTEL_ID);
    resetRoomType();
    resetRoom(ROOM_ID, "301");
    resetRoom(SECOND_ROOM_ID, "302");
    insertMember(HOTEL_ID, MANAGER_ID, "HOTEL_MANAGER");
    insertMember(OTHER_HOTEL_ID, OTHER_MANAGER_ID, "HOTEL_MANAGER");
    insertMember(HOTEL_ID, RECEPTIONIST_ID, "RECEPTIONIST");
  }

  @Nested
  @DisplayName("PATCH /api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}")
  class UpdateRoomTypeApiTests {

    @Test
    void shouldPersistChangedFieldsAndKeepOthersWhenOwnerUpdatesRoomType() throws Exception {
      // When
      patchJson(
              ROOM_TYPE_PATH,
              """
              {"name": "  Deluxe Hướng Biển  ", "maxAdults": 3, "maxOccupancy": 4,
               "basePrice": 1800000}
              """)
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("Cập nhật loại phòng thành công."))
          .andExpect(jsonPath("$.data.name").value("Deluxe Hướng Biển"))
          .andExpect(jsonPath("$.data.maxAdults").value(3))
          .andExpect(jsonPath("$.data.maxChildren").value(1))
          .andExpect(jsonPath("$.data.maxOccupancy").value(4))
          .andExpect(jsonPath("$.data.basePrice").value(1800000))
          .andExpect(jsonPath("$.data.active").value(true));

      // Then
      Map<String, Object> row =
          jdbc.queryForMap(
              """
              SELECT name, description, max_adults, max_children, max_occupancy, base_price,
                     is_active, created_at, updated_at, version
              FROM room_types WHERE id = ?
              """,
              ROOM_TYPE_ID);
      assertEquals("Deluxe Hướng Biển", row.get("name"));
      assertEquals("Mô tả gốc", row.get("description"));
      assertEquals((short) 3, ((Number) row.get("max_adults")).shortValue());
      assertEquals((short) 1, ((Number) row.get("max_children")).shortValue());
      assertEquals((short) 4, ((Number) row.get("max_occupancy")).shortValue());
      assertEquals(1_800_000L, ((Number) row.get("base_price")).longValue());
      assertEquals(Timestamp.from(BASELINE), row.get("created_at"));
      assertEquals(1L, ((Number) row.get("version")).longValue());
    }

    @Test
    void shouldStopSellingRoomTypeWhenActiveIsFalse() throws Exception {
      // When
      patchJson(ROOM_TYPE_PATH, "{\"active\": false}")
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.active").value(false));

      // Then
      assertEquals(
          Boolean.FALSE,
          jdbc.queryForObject(
              "SELECT is_active FROM room_types WHERE id = ?", Boolean.class, ROOM_TYPE_ID));
    }

    @Test
    void shouldRejectAndKeepRowWhenAdultsExceedCurrentOccupancy() throws Exception {
      // When
      patchJson(ROOM_TYPE_PATH, "{\"maxAdults\": 3}")
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_OCCUPANCY"))
          .andExpect(
              jsonPath("$.detail")
                  .value("Số người lớn (3) không được vượt quá sức chứa tối đa (2)."));

      // Then
      assertEquals(
          0L,
          jdbc.queryForObject(
              "SELECT version FROM room_types WHERE id = ?", Long.class, ROOM_TYPE_ID));
    }

    @Test
    void shouldReturnNotFoundWhenRoomTypeBelongsToAnotherHotel() throws Exception {
      // When / Then
      patchJson(
              "/api/v1/management/hotels/" + OTHER_HOTEL_ID + "/room-types/" + ROOM_TYPE_ID,
              "{\"name\": \"Không được sửa\"}")
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("ROOM_TYPE_NOT_FOUND"));
    }

    @Test
    void shouldRejectRequestWhenNoTokenIsProvided() throws Exception {
      // When / Then
      mvc.perform(
              patch(ROOM_TYPE_PATH)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"name\": \"X\"}"))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PATCH /api/v1/management/hotels/{hotelId}/rooms/{roomId}")
  class UpdateRoomApiTests {

    @Test
    void shouldPersistRoomNumberFloorAndDirtyReadinessWhenOwnerUpdatesRoom() throws Exception {
      // When
      patchJson(ROOM_PATH, "{\"roomNumber\": \" 305 \", \"floor\": 3, \"status\": \"cleaning\"}")
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.message").value("Cập nhật phòng thành công."))
          .andExpect(jsonPath("$.data.roomNumber").value("305"))
          .andExpect(jsonPath("$.data.floor").value(3))
          .andExpect(jsonPath("$.data.status").value("cleaning"))
          .andExpect(jsonPath("$.data.active").value(true));

      // Then
      Map<String, Object> row =
          jdbc.queryForMap(
              """
              SELECT room_number, floor, readiness::text AS readiness, readiness_updated_at,
                     operational_until, created_at, version
              FROM rooms WHERE id = ?
              """,
              ROOM_ID);
      assertEquals("305", row.get("room_number"));
      assertEquals("3", row.get("floor"));
      assertEquals("DIRTY", row.get("readiness"));
      assertEquals(Timestamp.from(BASELINE), row.get("readiness_updated_at"));
      assertNull(row.get("operational_until"));
      assertEquals(Timestamp.from(BASELINE), row.get("created_at"));
      assertEquals(1L, ((Number) row.get("version")).longValue());
    }

    @Test
    void shouldReturnConflictAndKeepRowWhenRoomNumberBelongsToAnotherRoom() throws Exception {
      // When
      patchJson(ROOM_PATH, "{\"roomNumber\": \"302\"}")
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("DUPLICATE_ROOM_NUMBER"))
          .andExpect(jsonPath("$.detail").value("Số phòng '302' đã tồn tại trong khách sạn này."));

      // Then
      assertEquals(
          "301",
          jdbc.queryForObject("SELECT room_number FROM rooms WHERE id = ?", String.class, ROOM_ID));
    }

    @Test
    void shouldSetAndClearOperationalUntilWhenRoomIsDeactivatedThenReactivated() throws Exception {
      // When
      patchJson(ROOM_PATH, "{\"active\": false}")
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.active").value(false));
      Timestamp deactivatedAt =
          jdbc.queryForObject(
              "SELECT operational_until FROM rooms WHERE id = ?", Timestamp.class, ROOM_ID);
      patchJson(ROOM_PATH, "{\"active\": true}")
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.active").value(true));

      // Then
      assertNotNull(deactivatedAt);
      assertNull(
          jdbc.queryForObject(
              "SELECT operational_until FROM rooms WHERE id = ?", Timestamp.class, ROOM_ID));
    }

    @Test
    void shouldReturnBadRequestWhenStatusIsUnknown() throws Exception {
      // When / Then
      patchJson(ROOM_PATH, "{\"status\": \"broken\"}")
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void shouldReturnNotFoundWhenRoomBelongsToAnotherHotel() throws Exception {
      // When / Then
      patchJson(
              "/api/v1/management/hotels/" + OTHER_HOTEL_ID + "/rooms/" + ROOM_ID, "{\"floor\": 9}")
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("Phân quyền PATCH loại phòng và phòng (cùng quy tắc PATCH khách sạn)")
  class AccessApiTests {

    private static final String ROOM_TYPE_BODY = "{\"name\": \"Đổi tên\"}";
    private static final String ROOM_BODY = "{\"floor\": 9}";

    @Test
    void shouldAllowAdminToUpdateRoomTypeAndRoomOfAnyHotel() throws Exception {
      // When / Then
      patchAs(ROOM_TYPE_PATH, ROOM_TYPE_BODY, actor(ADMIN_ID, "ADMIN")).andExpect(status().isOk());
      patchAs(ROOM_PATH, ROOM_BODY, actor(ADMIN_ID, "ADMIN")).andExpect(status().isOk());
    }

    @Test
    void shouldAllowActiveHotelManagerToUpdateRoomTypeAndRoom() throws Exception {
      // When / Then
      patchAs(ROOM_TYPE_PATH, ROOM_TYPE_BODY, actor(MANAGER_ID, "CUSTOMER"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.name").value("Đổi tên"));
      patchAs(ROOM_PATH, ROOM_BODY, actor(MANAGER_ID, "CUSTOMER"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.floor").value(9));
    }

    @Test
    void shouldReturnForbiddenAndKeepRowsWhenActorIsReceptionist() throws Exception {
      // When / Then
      patchAs(ROOM_TYPE_PATH, ROOM_TYPE_BODY, actor(RECEPTIONIST_ID, "CUSTOMER"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
      patchAs(ROOM_PATH, ROOM_BODY, actor(RECEPTIONIST_ID, "CUSTOMER"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

      // Then
      assertEquals(
          "Standard",
          jdbc.queryForObject(
              "SELECT name FROM room_types WHERE id = ?", String.class, ROOM_TYPE_ID));
      assertEquals(
          "3", jdbc.queryForObject("SELECT floor FROM rooms WHERE id = ?", String.class, ROOM_ID));
    }

    @Test
    void shouldReturnNotFoundWhenActorManagesAnotherHotel() throws Exception {
      // When / Then
      patchAs(ROOM_TYPE_PATH, ROOM_TYPE_BODY, actor(OTHER_MANAGER_ID, "CUSTOMER"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
      patchAs(ROOM_PATH, ROOM_BODY, actor(OTHER_MANAGER_ID, "CUSTOMER"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
    }

    @Test
    void shouldReturnNotFoundAndKeepRowsWhenActorIsCustomer() throws Exception {
      // When / Then
      patchAs(ROOM_TYPE_PATH, ROOM_TYPE_BODY, actor(CUSTOMER_ID, "CUSTOMER"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
      patchAs(ROOM_PATH, ROOM_BODY, actor(CUSTOMER_ID, "CUSTOMER"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));

      // Then
      assertEquals(
          0L,
          jdbc.queryForObject(
              "SELECT version FROM room_types WHERE id = ?", Long.class, ROOM_TYPE_ID));
      assertEquals(
          0L, jdbc.queryForObject("SELECT version FROM rooms WHERE id = ?", Long.class, ROOM_ID));
    }

    @Test
    void shouldReturnNotFoundWhenAdminTargetsMissingHotel() throws Exception {
      // When / Then
      patchAs(
              "/api/v1/management/hotels/" + MISSING_HOTEL_ID + "/room-types/" + ROOM_TYPE_ID,
              ROOM_TYPE_BODY,
              actor(ADMIN_ID, "ADMIN"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
      patchAs(
              "/api/v1/management/hotels/" + MISSING_HOTEL_ID + "/rooms/" + ROOM_ID,
              ROOM_BODY,
              actor(ADMIN_ID, "ADMIN"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
    }
  }

  private ResultActions patchAs(String uri, String body, JwtRequestPostProcessor actor)
      throws Exception {
    return mvc.perform(
        patch(uri).with(actor).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private static JwtRequestPostProcessor actor(UUID userId, String role) {
    return jwt()
        .jwt(token -> token.subject(userId.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  private void insertUser(UUID id, String roleKey) {
    jdbc.update(
        """
        INSERT INTO users (id, email, password_hash, full_name, phone, status, email_verified_at)
        VALUES (?, ?, 'not-a-real-hash', 'Người dùng test', ?, 'active', now())
        ON CONFLICT (id) DO NOTHING
        """,
        id,
        "room-update-" + id + "@test.local",
        // phone UNIQUE: dải +84933... riêng của lớp này, lấy 6 số cuối của id.
        "+84933" + id.toString().substring(30));
    jdbc.update(
        """
        INSERT INTO user_roles (user_id, role_id)
        SELECT ?, id FROM roles WHERE role_key = ?
        ON CONFLICT (user_id, role_id) DO NOTHING
        """,
        id,
        roleKey);
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

  private ResultActions patchJson(String uri, String body) throws Exception {
    return mvc.perform(
        patch(uri).with(owner()).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private static JwtRequestPostProcessor owner() {
    return jwt()
        .jwt(token -> token.subject(OWNER_ID.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_HOTEL_OWNER"));
  }

  private void insertOwner() {
    jdbc.update(
        """
        INSERT INTO users (id, email, password_hash, full_name, phone, status, email_verified_at)
        VALUES (?, 'room-update-owner@test.local', 'not-a-real-hash', 'Chủ khách sạn test',
                '+84933000501', 'active', now())
        ON CONFLICT (id) DO NOTHING
        """,
        OWNER_ID);
    jdbc.update(
        """
        INSERT INTO user_roles (user_id, role_id)
        SELECT ?, id FROM roles WHERE role_key = 'HOTEL_OWNER'
        ON CONFLICT (user_id, role_id) DO NOTHING
        """,
        OWNER_ID);
  }

  private void insertHotel(UUID id) {
    jdbc.update(
        """
        INSERT INTO hotels (id, owner_user_id, city_id, name, slug, address)
        VALUES (?, ?, ?, 'Khách sạn sửa phòng', ?, '5 Trần Phú')
        ON CONFLICT (id) DO NOTHING
        """,
        id,
        OWNER_ID,
        DA_NANG_ID,
        "room-update-" + id);
  }

  private void resetRoomType() {
    jdbc.update(
        """
        INSERT INTO room_types (id, hotel_id, name, description, max_adults, max_children,
                                max_occupancy, base_price, is_active, created_at, updated_at,
                                version)
        VALUES (?, ?, 'Standard', 'Mô tả gốc', 2, 1, 2, 1000000, true, ?, ?, 0)
        ON CONFLICT (id) DO UPDATE SET
          name = EXCLUDED.name, description = EXCLUDED.description,
          max_adults = EXCLUDED.max_adults, max_children = EXCLUDED.max_children,
          max_occupancy = EXCLUDED.max_occupancy, base_price = EXCLUDED.base_price,
          is_active = EXCLUDED.is_active, created_at = EXCLUDED.created_at,
          updated_at = EXCLUDED.updated_at, version = EXCLUDED.version
        """,
        ROOM_TYPE_ID,
        HOTEL_ID,
        Timestamp.from(BASELINE),
        Timestamp.from(BASELINE));
  }

  private void resetRoom(UUID id, String roomNumber) {
    jdbc.update(
        """
        INSERT INTO rooms (id, hotel_id, room_type_id, room_number, floor, readiness,
                           readiness_updated_at, operational_from, operational_until,
                           created_at, updated_at, version)
        VALUES (?, ?, ?, ?, '3', 'READY', ?, ?, NULL, ?, ?, 0)
        ON CONFLICT (id) DO UPDATE SET
          room_number = EXCLUDED.room_number, floor = EXCLUDED.floor,
          readiness = EXCLUDED.readiness, readiness_updated_at = EXCLUDED.readiness_updated_at,
          operational_from = EXCLUDED.operational_from, operational_until = NULL,
          created_at = EXCLUDED.created_at, updated_at = EXCLUDED.updated_at,
          version = EXCLUDED.version
        """,
        id,
        HOTEL_ID,
        ROOM_TYPE_ID,
        roomNumber,
        Timestamp.from(BASELINE),
        Timestamp.from(BASELINE),
        Timestamp.from(BASELINE),
        Timestamp.from(BASELINE));
  }
}
