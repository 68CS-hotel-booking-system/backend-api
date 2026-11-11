package com.team.hotelbooking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// Đặt ở package gốc vì TestcontainersConfiguration là package-private.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Tiện ích khách sạn/loại phòng và sửa thông tin khách sạn")
class HotelAmenityIntegrationTests {

  private static final UUID ADMIN_ID = UUID.fromString("55555555-eeee-4eee-8eee-000000000001");
  private static final UUID OWNER_ID = UUID.fromString("55555555-eeee-4eee-8eee-000000000002");
  private static final UUID OTHER_OWNER_ID =
      UUID.fromString("55555555-eeee-4eee-8eee-000000000003");
  private static final UUID MANAGER_ID = UUID.fromString("55555555-eeee-4eee-8eee-000000000004");
  private static final UUID INACTIVE_MANAGER_ID =
      UUID.fromString("55555555-eeee-4eee-8eee-000000000005");
  private static final UUID CUSTOMER_ID = UUID.fromString("55555555-eeee-4eee-8eee-000000000006");
  private static final UUID OTHER_HOTEL_MANAGER_ID =
      UUID.fromString("55555555-eeee-4eee-8eee-000000000007");
  private static final UUID RECEPTIONIST_ID =
      UUID.fromString("55555555-eeee-4eee-8eee-000000000008");
  private static final UUID HOTEL_ID = UUID.fromString("66666666-ffff-4fff-8fff-000000000001");
  private static final UUID OTHER_HOTEL_ID =
      UUID.fromString("66666666-ffff-4fff-8fff-000000000002");
  private static final UUID DELUXE_ID = UUID.fromString("77777777-aaaa-4aaa-9aaa-000000000001");
  private static final UUID STANDARD_ID = UUID.fromString("77777777-aaaa-4aaa-9aaa-000000000002");
  private static final UUID OTHER_HOTEL_TYPE_ID =
      UUID.fromString("77777777-aaaa-4aaa-9aaa-000000000003");
  private static final UUID DA_NANG_ID = UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3");

  // Amenity seed sẵn trong migration V20261104191000000.
  private static final UUID POOL_ID = UUID.fromString("c290e20c-7bd7-440b-9b7d-38b0f9c359c3");
  private static final UUID PARKING_ID = UUID.fromString("59fe1090-6e67-4d93-b140-232c5e885c27");
  private static final UUID WIFI_ID = UUID.fromString("4e10ab03-f280-4717-a8ef-0155f186c41a");
  private static final UUID BALCONY_ID = UUID.fromString("76fac790-5753-4608-b2cd-a3fc4faaa281");

  private static final String HOTEL_PATH = "/api/v1/management/hotels/" + HOTEL_ID;

  @Autowired private MockMvc mvc;

  @Autowired private ObjectMapper mapper;

  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void setUp() {
    // Fixture chỉ dành cho test; chạy lại nhiều lần nhờ ON CONFLICT và reset trạng thái bên dưới.
    insertUser(ADMIN_ID, "ADMIN");
    insertUser(OWNER_ID, "HOTEL_OWNER");
    insertUser(OTHER_OWNER_ID, "HOTEL_OWNER");
    insertUser(MANAGER_ID, "CUSTOMER");
    insertUser(INACTIVE_MANAGER_ID, "CUSTOMER");
    insertUser(CUSTOMER_ID, "CUSTOMER");
    insertUser(OTHER_HOTEL_MANAGER_ID, "CUSTOMER");
    insertUser(RECEPTIONIST_ID, "CUSTOMER");
    insertHotel(HOTEL_ID, OWNER_ID);
    insertHotel(OTHER_HOTEL_ID, OTHER_OWNER_ID);
    insertRoomType(DELUXE_ID, HOTEL_ID, "Deluxe King");
    insertRoomType(STANDARD_ID, HOTEL_ID, "Standard Twin");
    insertRoomType(OTHER_HOTEL_TYPE_ID, OTHER_HOTEL_ID, "Phòng hotel khác");
    insertMember(HOTEL_ID, MANAGER_ID, "HOTEL_MANAGER", true);
    insertMember(HOTEL_ID, INACTIVE_MANAGER_ID, "HOTEL_MANAGER", false);
    insertMember(OTHER_HOTEL_ID, OTHER_HOTEL_MANAGER_ID, "HOTEL_MANAGER", true);
    insertMember(HOTEL_ID, RECEPTIONIST_ID, "RECEPTIONIST", true);

    jdbc.update(
        """
        UPDATE hotels
        SET name = 'Khách sạn Tiện Ích', description = NULL, star_rating = NULL,
            check_in_time = '14:00:00', version = 0
        WHERE id IN (?, ?)
        """,
        HOTEL_ID,
        OTHER_HOTEL_ID);
    jdbc.update("DELETE FROM hotel_amenities WHERE hotel_id IN (?, ?)", HOTEL_ID, OTHER_HOTEL_ID);
    jdbc.update(
        "DELETE FROM room_type_amenities WHERE room_type_id IN (?, ?, ?)",
        DELUXE_ID,
        STANDARD_ID,
        OTHER_HOTEL_TYPE_ID);
  }

  @Nested
  @DisplayName("Migration hotel_amenities và room_type_amenities")
  class MigrationTests {

    @Test
    void shouldRejectRoomAmenityWhenInsertingIntoHotelAmenities() {
      // When / Then
      assertThrows(
          DataIntegrityViolationException.class,
          () ->
              jdbc.update(
                  "INSERT INTO hotel_amenities (hotel_id, amenity_id) VALUES (?, ?)",
                  HOTEL_ID,
                  WIFI_ID));
    }

    @Test
    void shouldRejectHotelAmenityWhenInsertingIntoRoomTypeAmenities() {
      // When / Then
      assertThrows(
          DataIntegrityViolationException.class,
          () ->
              jdbc.update(
                  "INSERT INTO room_type_amenities (room_type_id, amenity_id) VALUES (?, ?)",
                  DELUXE_ID,
                  POOL_ID));
    }

    @Test
    void shouldRejectForcedCategoryWhenFixedColumnIsOverridden() {
      // When / Then
      // Ghi tay amenity_category = 'ROOM' để lách FK kép vẫn bị CHECK chặn.
      assertThrows(
          DataIntegrityViolationException.class,
          () ->
              jdbc.update(
                  """
                  INSERT INTO hotel_amenities (hotel_id, amenity_id, amenity_category)
                  VALUES (?, ?, 'ROOM')
                  """,
                  HOTEL_ID,
                  WIFI_ID));
    }

    @Test
    void shouldRejectDuplicatePairWhenSameAmenityIsInsertedTwice() {
      // Given
      jdbc.update(
          "INSERT INTO hotel_amenities (hotel_id, amenity_id) VALUES (?, ?)", HOTEL_ID, POOL_ID);

      // When / Then
      assertThrows(
          DataIntegrityViolationException.class,
          () ->
              jdbc.update(
                  "INSERT INTO hotel_amenities (hotel_id, amenity_id) VALUES (?, ?)",
                  HOTEL_ID,
                  POOL_ID));
    }

    @Test
    void shouldRejectUnknownAmenityWhenInsertingLink() {
      // When / Then
      assertThrows(
          DataIntegrityViolationException.class,
          () ->
              jdbc.update(
                  "INSERT INTO room_type_amenities (room_type_id, amenity_id) VALUES (?, ?)",
                  DELUXE_ID,
                  UUID.fromString("00000000-0000-4000-8000-000000000099")));
    }

    @Test
    void shouldDeleteLinksWhenRoomTypeIsDeleted() {
      // Given
      UUID tempTypeId = UUID.fromString("77777777-aaaa-4aaa-9aaa-000000000099");
      insertRoomType(tempTypeId, HOTEL_ID, "Phòng tạm");
      jdbc.update(
          "INSERT INTO room_type_amenities (room_type_id, amenity_id) VALUES (?, ?)",
          tempTypeId,
          WIFI_ID);

      // When
      jdbc.update("DELETE FROM room_types WHERE id = ?", tempTypeId);

      // Then
      assertEquals(
          0,
          jdbc.queryForObject(
              "SELECT count(*) FROM room_type_amenities WHERE room_type_id = ?",
              Integer.class,
              tempTypeId));
    }

    @Test
    void shouldCreateAmenityIdIndexesWhenMigrationRuns() {
      // When
      List<String> indexes =
          jdbc.queryForList(
              """
              SELECT indexname FROM pg_indexes
              WHERE tablename IN ('hotel_amenities', 'room_type_amenities')
              ORDER BY indexname
              """,
              String.class);

      // Then
      assertTrue(indexes.contains("idx_hotel_amenities_amenity_id"), indexes.toString());
      assertTrue(indexes.contains("idx_room_type_amenities_amenity_id"), indexes.toString());
      assertTrue(indexes.contains("pk_hotel_amenities"), indexes.toString());
      assertTrue(indexes.contains("pk_room_type_amenities"), indexes.toString());
    }
  }

  @Nested
  @DisplayName("PATCH /api/v1/management/hotels/{hotelId}")
  class UpdateHotelApiTests {

    private static final String BODY = "{\"name\":\"Tên do người sửa\",\"version\":0}";

    @Test
    void shouldUpdateHotelWhenActorIsAdmin() throws Exception {
      // When
      JsonNode data = readData(patchHotel(as(ADMIN_ID, "ADMIN"), BODY), 200);

      // Then
      // ADMIN không sở hữu và không thuộc hotel_members của hotel này.
      assertEquals("Tên do người sửa", data.get("name").asString());
    }

    @Test
    void shouldUpdateFieldsAndIncreaseVersionWhenActorOwnsHotel() throws Exception {
      // When
      JsonNode data =
          readData(
              patchHotel(
                  as(OWNER_ID, "HOTEL_OWNER"),
                  """
                  {"name":"  Khách sạn Đổi Tên  ","starRating":4,
                   "checkInTime":"13:00:00","version":0}
                  """),
              200);

      // Then
      assertEquals("Khách sạn Đổi Tên", data.get("name").asString());
      assertEquals(4, data.get("starRating").asInt());
      assertEquals("13:00:00", data.get("checkInTime").asString());
      assertEquals(1, data.get("version").asLong());
      Map<String, Object> row =
          jdbc.queryForMap("SELECT name, slug, version FROM hotels WHERE id = ?", HOTEL_ID);
      assertEquals("Khách sạn Đổi Tên", row.get("name"));
      assertEquals("amenity-" + HOTEL_ID, row.get("slug"));
      assertEquals(1L, ((Number) row.get("version")).longValue());
    }

    @Test
    void shouldUpdateHotelWhenActorIsActiveManagerOfSameHotel() throws Exception {
      // When
      JsonNode data =
          readData(
              patchHotel(
                  as(MANAGER_ID, "CUSTOMER"),
                  "{\"description\":\"Mô tả do quản lý sửa\",\"version\":0}"),
              200);

      // Then
      assertEquals("Mô tả do quản lý sửa", data.get("description").asString());
    }

    @Test
    void shouldReturnNotFoundWhenActorManagesAnotherHotel() throws Exception {
      // When / Then
      patchHotel(as(OTHER_HOTEL_MANAGER_ID, "CUSTOMER"), BODY)
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
      assertHotelUnchanged();
    }

    @Test
    void shouldDenyAccessWhenActorIsReceptionistOfHotel() throws Exception {
      // When / Then
      patchHotel(as(RECEPTIONIST_ID, "CUSTOMER"), BODY)
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
      assertHotelUnchanged();
    }

    @Test
    void shouldReturnNotFoundWhenActorIsCustomer() throws Exception {
      // When / Then
      patchHotel(as(CUSTOMER_ID, "CUSTOMER"), BODY)
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
      assertHotelUnchanged();
    }

    @Test
    void shouldReturnNotFoundWhenOwnerUpdatesAnotherOwnersHotel() throws Exception {
      // When / Then
      patchHotel(as(OTHER_OWNER_ID, "HOTEL_OWNER"), BODY)
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
      assertHotelUnchanged();
    }

    @Test
    void shouldReturnNotFoundWhenManagerMembershipIsInactive() throws Exception {
      // When / Then
      // Thành viên đã ngừng hoạt động không còn là thành viên hotel.
      patchHotel(as(INACTIVE_MANAGER_ID, "CUSTOMER"), BODY)
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
    }

    @Test
    void shouldReturnConflictWithoutChangingRowWhenVersionIsStale() throws Exception {
      // Given
      jdbc.update("UPDATE hotels SET version = 5 WHERE id = ?", HOTEL_ID);

      // When / Then
      patchHotel(as(OWNER_ID, "HOTEL_OWNER"), "{\"name\":\"Ghi đè\",\"version\":4}")
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("CONCURRENT_UPDATE"));
      assertHotelUnchanged();
    }

    @Test
    void shouldReturnNotFoundWhenAdminUpdatesMissingHotel() throws Exception {
      // When / Then
      mvc.perform(
              patch("/api/v1/management/hotels/66666666-ffff-4fff-8fff-000000000099")
                  .with(as(ADMIN_ID, "ADMIN"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(BODY))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
    }

    @Test
    void shouldRejectRequestWhenOnlyLatitudeIsProvided() throws Exception {
      // When / Then
      patchHotel(as(OWNER_ID, "HOTEL_OWNER"), "{\"latitude\":10.5,\"version\":0}")
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
          .andExpect(jsonPath("$.detail").value("Vĩ độ và kinh độ phải được gửi cùng nhau."));
    }

    @Test
    void shouldRejectRequestWhenNoTokenIsProvided() throws Exception {
      // When / Then
      mvc.perform(patch(HOTEL_PATH).contentType(MediaType.APPLICATION_JSON).content(BODY))
          .andExpect(status().isUnauthorized());
    }

    private ResultActions patchHotel(JwtRequestPostProcessor actor, String body) throws Exception {
      return mvc.perform(
          patch(HOTEL_PATH).with(actor).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void assertHotelUnchanged() {
      assertEquals(
          "Khách sạn Tiện Ích",
          jdbc.queryForObject("SELECT name FROM hotels WHERE id = ?", String.class, HOTEL_ID));
    }
  }

  @Nested
  @DisplayName("PUT /api/v1/management/hotels/{hotelId}/amenities")
  class ReplaceHotelAmenitiesApiTests {

    @Test
    void shouldReplaceWholeListWhenOwnerSendsNewList() throws Exception {
      // Given
      putAmenities(HOTEL_PATH + "/amenities", as(OWNER_ID, "HOTEL_OWNER"), POOL_ID, PARKING_ID);

      // When
      JsonNode data =
          readData(
              putAmenities(HOTEL_PATH + "/amenities", as(OWNER_ID, "HOTEL_OWNER"), PARKING_ID),
              200);

      // Then
      assertEquals(List.of(PARKING_ID.toString()), ids(data));
      assertEquals(
          List.of(PARKING_ID),
          jdbc.queryForList(
              "SELECT amenity_id FROM hotel_amenities WHERE hotel_id = ?", UUID.class, HOTEL_ID));
    }

    @Test
    void shouldReturnAmenitiesInDatabaseNameOrderWhenListIsSaved() throws Exception {
      // Given
      List<String> expected =
          jdbc.queryForList(
              "SELECT id::text FROM amenities WHERE id IN (?, ?) ORDER BY name, id",
              String.class,
              POOL_ID,
              PARKING_ID);

      // When
      JsonNode data =
          readData(
              putAmenities(
                  HOTEL_PATH + "/amenities", as(MANAGER_ID, "CUSTOMER"), POOL_ID, PARKING_ID),
              200);

      // Then
      assertEquals(expected, ids(data));
    }

    @Test
    void shouldKeepOldListWhenRequestContainsRoomAmenity() throws Exception {
      // Given
      putAmenities(HOTEL_PATH + "/amenities", as(OWNER_ID, "HOTEL_OWNER"), POOL_ID);

      // When / Then
      putAmenities(HOTEL_PATH + "/amenities", as(OWNER_ID, "HOTEL_OWNER"), PARKING_ID, WIFI_ID)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("AMENITY_CATEGORY_MISMATCH"));
      assertEquals(
          List.of(POOL_ID),
          jdbc.queryForList(
              "SELECT amenity_id FROM hotel_amenities WHERE hotel_id = ?", UUID.class, HOTEL_ID));
    }

    @Test
    void shouldReturnNotFoundWhenAmenityDoesNotExist() throws Exception {
      // When / Then
      putAmenities(
              HOTEL_PATH + "/amenities",
              as(ADMIN_ID, "ADMIN"),
              UUID.fromString("00000000-0000-4000-8000-000000000099"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("AMENITY_NOT_FOUND"));
    }

    @Test
    void shouldDenyAccessWithoutChangesWhenActorIsReceptionistOfHotel() throws Exception {
      // When / Then
      putAmenities(HOTEL_PATH + "/amenities", as(RECEPTIONIST_ID, "CUSTOMER"), POOL_ID)
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
      assertEquals(
          0,
          jdbc.queryForObject(
              "SELECT count(*) FROM hotel_amenities WHERE hotel_id = ?", Integer.class, HOTEL_ID));
    }

    @Test
    void shouldReturnNotFoundWhenOtherHotelManagerReplacesHotelAmenities() throws Exception {
      // When / Then
      putAmenities(HOTEL_PATH + "/amenities", as(OTHER_HOTEL_MANAGER_ID, "CUSTOMER"), POOL_ID)
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
    }

    @Test
    void shouldReturnNotFoundWhenCustomerReplacesHotelAmenities() throws Exception {
      // When / Then
      putAmenities(HOTEL_PATH + "/amenities", as(CUSTOMER_ID, "CUSTOMER"), POOL_ID)
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
    }

    @Test
    void shouldReturnNotFoundWhenOwnerReplacesAnotherOwnersHotelAmenities() throws Exception {
      // When / Then
      putAmenities(HOTEL_PATH + "/amenities", as(OTHER_OWNER_ID, "HOTEL_OWNER"), POOL_ID)
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("PUT /api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}/amenities")
  class ReplaceRoomTypeAmenitiesApiTests {

    @Test
    void shouldReplaceListWhenManagerSendsRoomAmenities() throws Exception {
      // When
      JsonNode data =
          readData(
              putAmenities(
                  HOTEL_PATH + "/room-types/" + DELUXE_ID + "/amenities",
                  as(MANAGER_ID, "CUSTOMER"),
                  WIFI_ID,
                  BALCONY_ID),
              200);

      // Then
      assertEquals(2, data.size());
      assertEquals(
          2,
          jdbc.queryForObject(
              "SELECT count(*) FROM room_type_amenities WHERE room_type_id = ?",
              Integer.class,
              DELUXE_ID));
    }

    @Test
    void shouldReturnBadRequestWhenRequestContainsHotelAmenity() throws Exception {
      // When / Then
      putAmenities(
              HOTEL_PATH + "/room-types/" + DELUXE_ID + "/amenities",
              as(OWNER_ID, "HOTEL_OWNER"),
              POOL_ID)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("AMENITY_CATEGORY_MISMATCH"));
    }

    @Test
    void shouldDenyAccessWhenReceptionistReplacesRoomTypeAmenities() throws Exception {
      // When / Then
      putAmenities(
              HOTEL_PATH + "/room-types/" + DELUXE_ID + "/amenities",
              as(RECEPTIONIST_ID, "CUSTOMER"),
              WIFI_ID)
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void shouldReturnNotFoundWhenCustomerReplacesRoomTypeAmenities() throws Exception {
      // When / Then
      putAmenities(
              HOTEL_PATH + "/room-types/" + DELUXE_ID + "/amenities",
              as(CUSTOMER_ID, "CUSTOMER"),
              WIFI_ID)
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
    }

    @Test
    void shouldReturnNotFoundWhenRoomTypeBelongsToAnotherHotel() throws Exception {
      // When / Then
      putAmenities(
              HOTEL_PATH + "/room-types/" + OTHER_HOTEL_TYPE_ID + "/amenities",
              as(OWNER_ID, "HOTEL_OWNER"),
              WIFI_ID)
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("ROOM_TYPE_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("GET /api/v1/hotels/{hotelId} và /room-types công khai")
  class PublicAmenitiesApiTests {

    @Test
    void shouldIncludeHotelAmenitiesWhenHotelIsPublic() throws Exception {
      // Given
      putAmenities(HOTEL_PATH + "/amenities", as(OWNER_ID, "HOTEL_OWNER"), POOL_ID);

      // When
      JsonNode data = readData(mvc.perform(get("/api/v1/hotels/" + HOTEL_ID)), 200);

      // Then
      JsonNode amenities = data.get("amenities");
      assertEquals(1, amenities.size());
      assertEquals(POOL_ID.toString(), amenities.get(0).get("id").asString());
      assertEquals("SWIMMING_POOL", amenities.get(0).get("code").asString());
      assertEquals("HOTEL", amenities.get(0).get("category").asString());
    }

    @Test
    void shouldIncludeAmenitiesPerRoomTypeWhenListingPublicRoomTypes() throws Exception {
      // Given
      putAmenities(
          HOTEL_PATH + "/room-types/" + DELUXE_ID + "/amenities",
          as(OWNER_ID, "HOTEL_OWNER"),
          WIFI_ID);

      // When
      JsonNode data = readData(mvc.perform(get("/api/v1/hotels/" + HOTEL_ID + "/room-types")), 200);

      // Then
      for (JsonNode roomType : data) {
        List<String> amenityIds = ids(roomType.get("amenities"));
        if (roomType.get("id").asString().equals(DELUXE_ID.toString())) {
          assertEquals(List.of(WIFI_ID.toString()), amenityIds);
        } else {
          assertEquals(List.of(), amenityIds, "Loại phòng chưa gắn tiện ích trả mảng rỗng");
        }
      }
    }
  }

  private ResultActions putAmenities(String uri, JwtRequestPostProcessor actor, UUID... amenityIds)
      throws Exception {
    String ids = String.join(",", Arrays.stream(amenityIds).map(id -> "\"" + id + "\"").toList());
    return mvc.perform(
        put(uri)
            .with(actor)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"amenityIds\":[" + ids + "]}"));
  }

  private JsonNode readData(ResultActions result, int expectedStatus) throws Exception {
    String body =
        result
            .andExpect(status().is(expectedStatus))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return mapper.readTree(body).get("data");
  }

  private static List<String> ids(JsonNode items) {
    return items.valueStream().map(item -> item.get("id").asString()).toList();
  }

  private static JwtRequestPostProcessor as(UUID userId, String role) {
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
        "amenity-" + id + "@test.local",
        // phone UNIQUE: dải +84922... không trùng fixture của các test khác.
        "+84922" + id.toString().substring(30));
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
        INSERT INTO hotels (id, owner_user_id, city_id, name, slug, address, status)
        VALUES (?, ?, ?, 'Khách sạn Tiện Ích', ?, '12 Võ Nguyên Giáp', 'ACTIVE')
        ON CONFLICT (id) DO NOTHING
        """,
        id,
        ownerId,
        DA_NANG_ID,
        "amenity-" + id);
  }

  private void insertRoomType(UUID id, UUID hotelId, String name) {
    jdbc.update(
        """
        INSERT INTO room_types (id, hotel_id, name, is_active)
        VALUES (?, ?, ?, TRUE)
        ON CONFLICT (id) DO NOTHING
        """,
        id,
        hotelId,
        name);
  }

  private void insertMember(UUID hotelId, UUID userId, String role, boolean active) {
    jdbc.update(
        """
        INSERT INTO hotel_members (hotel_id, user_id, role, is_active)
        VALUES (?, ?, CAST(? AS hotel_member_role__t), ?)
        ON CONFLICT (hotel_id, user_id, role) DO UPDATE SET is_active = EXCLUDED.is_active
        """,
        hotelId,
        userId,
        role,
        active);
  }
}
