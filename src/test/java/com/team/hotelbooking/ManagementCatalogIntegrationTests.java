package com.team.hotelbooking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// Đặt ở package gốc vì TestcontainersConfiguration là package-private.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Danh sách loại phòng và phòng vật lý quản lý")
class ManagementCatalogIntegrationTests {

  private static final UUID ADMIN_ID = UUID.fromString("11111111-aaaa-4aaa-8aaa-000000000001");
  private static final UUID OWNER_ID = UUID.fromString("11111111-aaaa-4aaa-8aaa-000000000002");
  private static final UUID OTHER_OWNER_ID =
      UUID.fromString("11111111-aaaa-4aaa-8aaa-000000000003");
  private static final UUID CUSTOMER_ID = UUID.fromString("11111111-aaaa-4aaa-8aaa-000000000004");
  private static final UUID HOTEL_ID = UUID.fromString("22222222-bbbb-4bbb-8bbb-000000000001");
  private static final UUID OTHER_HOTEL_ID =
      UUID.fromString("22222222-bbbb-4bbb-8bbb-000000000002");
  private static final UUID ACTIVE_TYPE_ID =
      UUID.fromString("33333333-cccc-4ccc-8ccc-000000000001");
  private static final UUID SUSPENDED_TYPE_ID =
      UUID.fromString("33333333-cccc-4ccc-8ccc-000000000002");
  private static final UUID OTHER_HOTEL_TYPE_ID =
      UUID.fromString("33333333-cccc-4ccc-8ccc-000000000003");
  private static final UUID DA_NANG_ID = UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3");
  private static final String ROOM_TYPES_PATH =
      "/api/v1/management/hotels/" + HOTEL_ID + "/room-types";
  private static final String ROOMS_PATH = "/api/v1/management/hotels/" + HOTEL_ID + "/rooms";

  @Autowired private MockMvc mvc;

  @Autowired private ObjectMapper mapper;

  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void setUp() {
    // Fixture chỉ dành cho test; chạy lại nhiều lần nhờ ON CONFLICT.
    insertUser(ADMIN_ID, "ADMIN");
    insertUser(OWNER_ID, "HOTEL_OWNER");
    insertUser(OTHER_OWNER_ID, "HOTEL_OWNER");
    insertUser(CUSTOMER_ID, "CUSTOMER");
    insertHotel(HOTEL_ID, OWNER_ID);
    insertHotel(OTHER_HOTEL_ID, OTHER_OWNER_ID);
    insertRoomType(ACTIVE_TYPE_ID, HOTEL_ID, "Standard Twin", true);
    insertRoomType(SUSPENDED_TYPE_ID, HOTEL_ID, "Deluxe King", false);
    insertRoomType(OTHER_HOTEL_TYPE_ID, OTHER_HOTEL_ID, "Phòng hotel khác", true);
    insertRoom("44444444-dddd-4ddd-8ddd-000000000001", HOTEL_ID, ACTIVE_TYPE_ID, "201", "2");
    insertRoom("44444444-dddd-4ddd-8ddd-000000000002", HOTEL_ID, ACTIVE_TYPE_ID, "101", "1");
    insertRoom("44444444-dddd-4ddd-8ddd-000000000003", HOTEL_ID, SUSPENDED_TYPE_ID, "102", "1");
    insertRoom("44444444-dddd-4ddd-8ddd-000000000004", HOTEL_ID, ACTIVE_TYPE_ID, "G1", null);
    insertRoom(
        "44444444-dddd-4ddd-8ddd-000000000005", OTHER_HOTEL_ID, OTHER_HOTEL_TYPE_ID, "101", "1");
  }

  @Nested
  @DisplayName("GET /api/v1/management/hotels/{hotelId}/room-types")
  class ListManagementRoomTypesApiTests {

    @Test
    void shouldReturnActiveAndSuspendedRoomTypesWhenOwnerManagesHotel() throws Exception {
      // Given
      List<String> expectedIds =
          jdbc.queryForList(
              "SELECT id::text FROM room_types WHERE hotel_id = ? ORDER BY name, id",
              String.class,
              HOTEL_ID);

      // When
      JsonNode data =
          readData(ROOM_TYPES_PATH, as(OWNER_ID, "HOTEL_OWNER"), "Cache-Control", "no-store");

      // Then
      assertEquals(expectedIds, ids(data));
      assertEquals(2, data.size());
      assertTrue(
          data.valueStream()
              .anyMatch(
                  item ->
                      item.get("id").asString().equals(SUSPENDED_TYPE_ID.toString())
                          && !item.get("active").asBoolean()),
          "Phải trả cả loại phòng ngừng bán với active=false");
    }

    @Test
    void shouldReadAnyHotelWhenActorIsAdmin() throws Exception {
      // When
      JsonNode data = readData(ROOM_TYPES_PATH, as(ADMIN_ID, "ADMIN"), null, null);

      // Then
      assertEquals(2, data.size());
    }

    @Test
    void shouldReturnNotFoundWhenOwnerRequestsAnotherOwnersHotel() throws Exception {
      // When / Then
      mvc.perform(get(ROOM_TYPES_PATH).with(as(OTHER_OWNER_ID, "HOTEL_OWNER")))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
    }

    @Test
    void shouldDenyAccessWhenActorIsCustomer() throws Exception {
      // When / Then
      mvc.perform(get(ROOM_TYPES_PATH).with(as(CUSTOMER_ID, "CUSTOMER")))
          .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectRequestWhenNoTokenIsProvided() throws Exception {
      // When / Then
      mvc.perform(get(ROOM_TYPES_PATH)).andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/management/hotels/{hotelId}/rooms")
  class ListManagementRoomsApiTests {

    @Test
    void shouldReturnRoomsInFloorRoomNumberOrderWhenOwnerManagesHotel() throws Exception {
      // Given
      // floor/room_number là chuỗi và phụ thuộc collation; kỳ vọng lấy từ chính DB test.
      List<String> expectedIds =
          jdbc.queryForList(
              """
              SELECT id::text FROM rooms WHERE hotel_id = ?
              ORDER BY floor ASC NULLS LAST, room_number, id
              """,
              String.class,
              HOTEL_ID);

      // When
      JsonNode page =
          readData(ROOMS_PATH, as(OWNER_ID, "HOTEL_OWNER"), "Cache-Control", "no-store");

      // Then
      assertEquals(expectedIds, ids(page.get("content")));
      assertEquals(4, page.get("totalElements").asInt());
      assertEquals(0, page.get("page").asInt());
      assertEquals(20, page.get("size").asInt());
    }

    @Test
    void shouldPlaceRoomsWithoutFloorLastAcrossPagesWhenSortingByFloor() throws Exception {
      // When
      JsonNode firstPage =
          readData(ROOMS_PATH + "?page=0&size=3", as(OWNER_ID, "HOTEL_OWNER"), null, null);
      JsonNode lastPage =
          readData(ROOMS_PATH + "?page=1&size=3", as(OWNER_ID, "HOTEL_OWNER"), null, null);

      // Then
      firstPage
          .get("content")
          .valueStream()
          .forEach(
              item -> assertFalse(item.get("floor").isNull(), "Trang đầu chỉ có phòng có tầng"));
      JsonNode noFloorRoom = lastPage.get("content").get(0);
      assertEquals(1, lastPage.get("content").size());
      assertTrue(noFloorRoom.get("floor").isNull(), "Phòng floor NULL nằm cuối danh sách");
      assertEquals("G1", noFloorRoom.get("roomNumber").asString());
    }

    @Test
    void shouldReturnOnlyRoomsOfRoomTypeWhenRoomTypeIdIsProvided() throws Exception {
      // When
      JsonNode page =
          readData(
              ROOMS_PATH + "?roomTypeId=" + ACTIVE_TYPE_ID,
              as(OWNER_ID, "HOTEL_OWNER"),
              null,
              null);

      // Then
      assertEquals(3, page.get("totalElements").asInt());
      page.get("content")
          .valueStream()
          .forEach(
              item -> assertEquals(ACTIVE_TYPE_ID.toString(), item.get("roomTypeId").asString()));
    }

    @Test
    void shouldReturnEmptyPageWhenRoomTypeBelongsToAnotherHotel() throws Exception {
      // When
      JsonNode page =
          readData(
              ROOMS_PATH + "?roomTypeId=" + OTHER_HOTEL_TYPE_ID,
              as(OWNER_ID, "HOTEL_OWNER"),
              null,
              null);

      // Then
      assertEquals(0, page.get("totalElements").asInt());
      assertEquals(0, page.get("content").size());
    }

    @Test
    void shouldSplitResultIntoPagesWhenSizeIsProvided() throws Exception {
      // When
      JsonNode page =
          readData(ROOMS_PATH + "?page=1&size=3", as(OWNER_ID, "HOTEL_OWNER"), null, null);

      // Then
      assertEquals(1, page.get("content").size());
      assertEquals(4, page.get("totalElements").asInt());
      assertEquals(2, page.get("totalPages").asInt());
      assertTrue(page.get("last").asBoolean());
    }

    @Test
    void shouldReturnBadRequestWhenRoomTypeIdIsNotUuid() throws Exception {
      // When / Then
      mvc.perform(get(ROOMS_PATH + "?roomTypeId=abc").with(as(OWNER_ID, "HOTEL_OWNER")))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldUseSameErrorCodeAsHotelListWhenQueryParameterHasWrongType() throws Exception {
      // When
      // HT07 của Đạt là chuẩn đối chiếu: sai kiểu query param đi qua handleTypeMismatch chung.
      JsonNode hotelList =
          readProblem("/api/v1/management/hotels?page=abc", as(OWNER_ID, "HOTEL_OWNER"));
      JsonNode rooms = readProblem(ROOMS_PATH + "?page=abc", as(OWNER_ID, "HOTEL_OWNER"));

      // Then
      assertEquals("BAD_REQUEST", hotelList.get("code").asString());
      assertEquals(hotelList.get("code"), rooms.get("code"));
      assertEquals(hotelList.get("status"), rooms.get("status"));
      assertEquals(hotelList.get("detail"), rooms.get("detail"));
    }

    @Test
    void shouldReturnNotFoundWhenOwnerRequestsAnotherOwnersHotel() throws Exception {
      // When / Then
      mvc.perform(get(ROOMS_PATH).with(as(OTHER_OWNER_ID, "HOTEL_OWNER")))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
    }

    @Test
    void shouldRejectRequestWhenNoTokenIsProvided() throws Exception {
      // When / Then
      mvc.perform(get(ROOMS_PATH)).andExpect(status().isUnauthorized());
    }
  }

  private static JwtRequestPostProcessor as(UUID userId, String role) {
    return jwt()
        .jwt(token -> token.subject(userId.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  private JsonNode readData(
      String uri, JwtRequestPostProcessor actor, String headerName, String headerValue)
      throws Exception {
    var result = mvc.perform(get(uri).with(actor)).andExpect(status().isOk());
    if (headerName != null) {
      result.andExpect(header().string(headerName, headerValue));
    }
    String body = result.andReturn().getResponse().getContentAsString();
    return mapper.readTree(body).get("data");
  }

  private JsonNode readProblem(String uri, JwtRequestPostProcessor actor) throws Exception {
    String body =
        mvc.perform(get(uri).with(actor))
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return mapper.readTree(body);
  }

  private static List<String> ids(JsonNode items) {
    return items.valueStream().map(item -> item.get("id").asString()).toList();
  }

  private void insertUser(UUID id, String roleKey) {
    jdbc.update(
        """
        INSERT INTO users (id, email, password_hash, full_name, phone, status, email_verified_at)
        VALUES (?, ?, 'not-a-real-hash', 'Người dùng test', ?, 'active', now())
        ON CONFLICT (id) DO NOTHING
        """,
        id,
        "management-" + id + "@test.local",
        // phone UNIQUE: lấy 6 số cuối của id, dải +84911... không trùng fixture test khác.
        "+84911" + id.toString().substring(30));
    jdbc.update(
        """
        INSERT INTO user_roles (user_id, role_id)
        SELECT ?, id FROM roles WHERE role_key = ?
        ON CONFLICT (user_id, role_id) DO NOTHING
        """,
        id,
        roleKey);
  }

  private void insertHotel(UUID id, UUID ownerId) {
    jdbc.update(
        """
        INSERT INTO hotels (id, owner_user_id, city_id, name, slug, address)
        VALUES (?, ?, ?, 'Khách sạn quản lý', ?, '12 Võ Nguyên Giáp')
        ON CONFLICT (id) DO NOTHING
        """,
        id,
        ownerId,
        DA_NANG_ID,
        "management-" + id);
  }

  private void insertRoomType(UUID id, UUID hotelId, String name, boolean active) {
    jdbc.update(
        """
        INSERT INTO room_types (id, hotel_id, name, is_active)
        VALUES (?, ?, ?, ?)
        ON CONFLICT (id) DO NOTHING
        """,
        id,
        hotelId,
        name,
        active);
  }

  private void insertRoom(
      String id, UUID hotelId, UUID roomTypeId, String roomNumber, String floor) {
    jdbc.update(
        """
        INSERT INTO rooms (id, hotel_id, room_type_id, room_number, floor)
        VALUES (CAST(? AS uuid), ?, ?, ?, ?)
        ON CONFLICT (id) DO NOTHING
        """,
        id,
        hotelId,
        roomTypeId,
        roomNumber,
        floor);
  }
}
