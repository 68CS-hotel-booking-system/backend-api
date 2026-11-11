package com.team.hotelbooking;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("API tiện ích riêng và tiện ích phòng vật lý với PostgreSQL")
class ScopedAmenityIntegrationTests {
  private static final UUID OWNER = UUID.fromString("55555555-abcd-4abc-8abc-111111110001");
  private static final UUID OTHER = UUID.fromString("55555555-abcd-4abc-8abc-111111110002");
  private static final UUID MANAGER = UUID.fromString("55555555-abcd-4abc-8abc-111111110003");
  private static final UUID RECEPTION = UUID.fromString("55555555-abcd-4abc-8abc-111111110004");
  private static final UUID ADMIN = UUID.fromString("55555555-abcd-4abc-8abc-111111110005");
  private static final UUID HOTEL = UUID.fromString("66666666-abcd-4abc-8abc-111111110001");
  private static final UUID HOTEL2 = UUID.fromString("66666666-abcd-4abc-8abc-111111110002");
  private static final UUID TYPE = UUID.fromString("77777777-abcd-4abc-8abc-111111110001");
  private static final UUID TYPE2 = UUID.fromString("77777777-abcd-4abc-8abc-111111110002");
  private static final UUID ROOM = UUID.fromString("88888888-abcd-4abc-8abc-111111110001");
  private static final UUID ROOM2 = UUID.fromString("88888888-abcd-4abc-8abc-111111110002");
  private static final UUID FOREIGN_ROOM = UUID.fromString("88888888-abcd-4abc-8abc-111111110003");
  private static final UUID CITY = UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3");
  private static final UUID POOL = UUID.fromString("c290e20c-7bd7-440b-9b7d-38b0f9c359c3");
  private static final UUID WIFI = UUID.fromString("4e10ab03-f280-4717-a8ef-0155f186c41a");
  private static final String BASE = "/api/v1/management/hotels/" + HOTEL;
  private static final String HOTEL_PATH = BASE + "/amenities";
  private static final String ROOM_PATH = BASE + "/rooms/" + ROOM + "/amenities";
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    user(OWNER, "HOTEL_OWNER");
    user(OTHER, "HOTEL_OWNER");
    user(MANAGER, "CUSTOMER");
    user(RECEPTION, "CUSTOMER");
    user(ADMIN, "ADMIN");
    hotel(HOTEL, OWNER);
    hotel(HOTEL2, OTHER);
    roomType(TYPE, HOTEL);
    roomType(TYPE2, HOTEL2);
    room(ROOM, HOTEL, TYPE, "S101");
    room(ROOM2, HOTEL, TYPE, "S102");
    room(FOREIGN_ROOM, HOTEL2, TYPE2, "S201");
    jdbc.update("UPDATE room_types SET is_active = true WHERE id IN (?, ?)", TYPE, TYPE2);
    member(MANAGER, "HOTEL_MANAGER");
    member(RECEPTION, "RECEPTIONIST");
    jdbc.update("DELETE FROM custom_amenities WHERE hotel_id IN (?, ?)", HOTEL, HOTEL2);
    jdbc.update("DELETE FROM room_amenities WHERE room_id IN (?, ?, ?)", ROOM, ROOM2, FOREIGN_ROOM);
    jdbc.update("DELETE FROM hotel_amenities WHERE hotel_id IN (?, ?)", HOTEL, HOTEL2);
    jdbc.update("DELETE FROM room_type_amenities WHERE room_type_id IN (?, ?)", TYPE, TYPE2);
  }

  @Nested
  @DisplayName("Đọc và tạo tiện ích riêng")
  class CreateAndRead {
    @Test
    void shouldReadEmptyAmenitiesWhenHotelIsDraft() throws Exception {
      request(get(HOTEL_PATH))
          .andExpect(status().isOk())
          .andExpect(header().string("Cache-Control", "no-store"))
          .andExpect(jsonPath("$.data.hotelId").value(HOTEL.toString()))
          .andExpect(jsonPath("$.data.roomId").isEmpty())
          .andExpect(jsonPath("$.data.catalogAmenities").isEmpty())
          .andExpect(jsonPath("$.data.customAmenities").isEmpty());
    }

    @Test
    void shouldCreateAndAttachHotelCustomAmenityWithoutChangingSharedCatalog() throws Exception {
      int before = jdbc.queryForObject("SELECT count(*) FROM amenities", Integer.class);
      var created = create(HOTEL_PATH, "Hồ bơi nước khoáng");
      assertTrue(created.get("attached").asBoolean());
      assertEquals(0, created.get("version").asLong());
      assertTrue(created.get("roomId").isNull());
      var loaded = data(request(get(HOTEL_PATH)), 200);
      assertEquals(
          created.get("id").asString(), loaded.get("customAmenities").get(0).get("id").asString());
      assertEquals(before, jdbc.queryForObject("SELECT count(*) FROM amenities", Integer.class));
      assertEquals(
          1,
          jdbc.queryForObject(
              "SELECT count(*) FROM custom_amenities WHERE hotel_id = ? AND attached",
              Integer.class,
              HOTEL));
    }

    @Test
    void shouldCreateAmenityOnlyForSelectedPhysicalRoom() throws Exception {
      var created = create(ROOM_PATH, "Bồn tắm riêng");
      assertEquals(ROOM.toString(), created.get("roomId").asString());
      assertEquals(1, data(request(get(ROOM_PATH)), 200).get("customAmenities").size());
      assertEquals(
          0,
          data(request(get(BASE + "/rooms/" + ROOM2 + "/amenities")), 200)
              .get("customAmenities")
              .size());
      assertEquals(0, data(request(get(HOTEL_PATH)), 200).get("customAmenities").size());
    }

    @Test
    void shouldRejectDuplicateNameWithinScopeEvenWhenCaseAndWhitespaceDiffer() throws Exception {
      create(HOTEL_PATH, "  Hồ   Bơi  ");
      request(post(HOTEL_PATH), Map.of("name", "hỒ BƠI"))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("DUPLICATE_CUSTOM_AMENITY"));
      assertEquals(
          1,
          jdbc.queryForObject(
              "SELECT count(*) FROM custom_amenities WHERE hotel_id = ?", Integer.class, HOTEL));
    }

    @Test
    void shouldAllowSameNameForDifferentRoomsOrHotels() throws Exception {
      create(HOTEL_PATH, "Tiện ích riêng");
      create(ROOM_PATH, "Tiện ích riêng");
      create(BASE + "/rooms/" + ROOM2 + "/amenities", "Tiện ích riêng");
      var result =
          mvc.perform(
              post("/api/v1/management/hotels/" + HOTEL2 + "/amenities")
                  .with(as(OTHER, "HOTEL_OWNER"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"name\":\"Tiện ích riêng\"}"));
      result.andExpect(status().isCreated());
    }

    @Test
    void shouldRejectInvalidBodyWithoutCreatingAnything() throws Exception {
      request(post(HOTEL_PATH), Map.of("name", " ")).andExpect(status().isBadRequest());
      request(post(HOTEL_PATH), Map.of("name", "x".repeat(101))).andExpect(status().isBadRequest());
      request(post(HOTEL_PATH), Map.of("name", "Tên", "description", "x".repeat(2001)))
          .andExpect(status().isBadRequest());
      assertEquals(
          0,
          jdbc.queryForObject(
              "SELECT count(*) FROM custom_amenities WHERE hotel_id = ?", Integer.class, HOTEL));
    }
  }

  @Nested
  @DisplayName("Sửa, tháo và gắn lại")
  class UpdateAndDetach {
    @Test
    void shouldEditDetachAndReattachSameAmenityWithVersions() throws Exception {
      String id = create(HOTEL_PATH, "Hồ bơi").get("id").asString();
      var edited =
          data(
              request(
                  patch(HOTEL_PATH + "/" + id),
                  Map.of(
                      "name", "Hồ bơi riêng", "description", "Nước khoáng", "expectedVersion", 0)),
              200);
      assertEquals(1, edited.get("version").asLong());
      var detached =
          data(request(delete(HOTEL_PATH + "/" + id).param("expectedVersion", "1")), 200);
      assertFalse(detached.get("attached").asBoolean());
      assertEquals(2, detached.get("version").asLong());
      assertFalse(
          data(request(get(HOTEL_PATH)), 200)
              .get("customAmenities")
              .get(0)
              .get("attached")
              .asBoolean());
      var reattached =
          data(
              request(patch(HOTEL_PATH + "/" + id), Map.of("attached", true, "expectedVersion", 2)),
              200);
      assertEquals(id, reattached.get("id").asString());
      assertTrue(reattached.get("attached").asBoolean());
      assertEquals(3, reattached.get("version").asLong());
    }

    @Test
    void shouldPreserveSavedStateWhenVersionIsStale() throws Exception {
      String id = create(ROOM_PATH, "Ban công").get("id").asString();
      request(patch(ROOM_PATH + "/" + id), Map.of("name", "Ban công lớn", "expectedVersion", 0))
          .andExpect(status().isOk());
      request(patch(ROOM_PATH + "/" + id), Map.of("name", "Tên cũ", "expectedVersion", 0))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("AMENITY_CONCURRENT_UPDATE"));
      request(delete(ROOM_PATH + "/" + id).param("expectedVersion", "0"))
          .andExpect(status().isConflict());
      assertEquals(
          "Ban công lớn",
          data(request(get(ROOM_PATH)), 200).get("customAmenities").get(0).get("name").asString());
    }

    @Test
    void shouldKeepVersionWhenAlreadyDetachedAndSameVersionIsSent() throws Exception {
      String id = create(ROOM_PATH, "Tiện ích").get("id").asString();
      request(delete(ROOM_PATH + "/" + id).param("expectedVersion", "0"))
          .andExpect(status().isOk());
      request(delete(ROOM_PATH + "/" + id).param("expectedVersion", "1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.version").value(1));
    }

    @Test
    void shouldRejectEmptyPatchAndMissingVersion() throws Exception {
      String id = create(HOTEL_PATH, "Tiện ích").get("id").asString();
      request(patch(HOTEL_PATH + "/" + id), Map.of("expectedVersion", 0))
          .andExpect(status().isBadRequest());
      request(patch(HOTEL_PATH + "/" + id), Map.of("name", "Tên mới"))
          .andExpect(status().isBadRequest());
      request(delete(HOTEL_PATH + "/" + id)).andExpect(status().isBadRequest());
      request(delete(HOTEL_PATH + "/" + id).param("expectedVersion", "-1"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectRenameToAnotherExistingCustomName() throws Exception {
      create(HOTEL_PATH, "Tên A");
      String id = create(HOTEL_PATH, "Tên B").get("id").asString();
      request(patch(HOTEL_PATH + "/" + id), Map.of("name", "Tên A", "expectedVersion", 0))
          .andExpect(status().isConflict());
      assertEquals(
          "Tên B",
          jdbc.queryForObject(
              "SELECT name FROM custom_amenities WHERE id = ?", String.class, UUID.fromString(id)));
    }
  }

  @Test
  @DisplayName("Đọc lại tiện ích hạng phòng ngừng bán khi khách sạn còn nháp")
  void shouldReadRoomTypeAmenitiesWhenDraftAndInactive() throws Exception {
    request(put(BASE + "/room-types/" + TYPE + "/amenities"), Map.of("amenityIds", List.of(WIFI)))
        .andExpect(status().isOk());
    jdbc.update("UPDATE room_types SET is_active = false WHERE id = ?", TYPE);
    request(get(BASE + "/room-types/" + TYPE + "/amenities"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(WIFI.toString()));
    jdbc.update("UPDATE room_types SET is_active = true WHERE id = ?", TYPE);
  }

  @Test
  @DisplayName("Không đọc tiện ích hạng phòng thuộc khách sạn khác")
  void shouldRejectRoomTypeReadForAnotherHotel() throws Exception {
    request(get(BASE + "/room-types/" + TYPE2 + "/amenities"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ROOM_TYPE_NOT_FOUND"));
  }

  @Test
  @DisplayName("Hai cập nhật cùng phiên bản chỉ một cập nhật được lưu")
  void shouldRejectOneConcurrentEditWhenBothUseSameVersion() throws Exception {
    String id = create(HOTEL_PATH, "Tên ban đầu").get("id").asString();
    try (var pool = Executors.newFixedThreadPool(2)) {
      Callable<Integer> edit =
          () ->
              request(patch(HOTEL_PATH + "/" + id), Map.of("name", "Tên mới", "expectedVersion", 0))
                  .andReturn()
                  .getResponse()
                  .getStatus();
      var statuses =
          pool.invokeAll(List.of(edit, edit)).stream()
              .map(
                  f -> {
                    try {
                      return f.get();
                    } catch (Exception e) {
                      throw new RuntimeException(e);
                    }
                  })
              .sorted()
              .toList();
      assertEquals(List.of(200, 409), statuses);
      assertEquals(
          1L,
          jdbc.queryForObject(
              "SELECT version FROM custom_amenities WHERE id = ?",
              Long.class,
              UUID.fromString(id)));
    }
  }

  @Nested
  @DisplayName("Danh mục có sẵn, tiện ích kế thừa và ràng buộc DB")
  class CatalogAndInheritance {
    @Test
    void shouldKeepCustomAndInheritedAmenitiesWhenDirectRoomSelectionIsCleared() throws Exception {
      create(ROOM_PATH, "Ban công đặc biệt");
      request(put(BASE + "/room-types/" + TYPE + "/amenities"), Map.of("amenityIds", List.of(WIFI)))
          .andExpect(status().isOk());
      var response = data(request(put(ROOM_PATH), Map.of("amenityIds", List.of(WIFI, WIFI))), 200);
      assertEquals(1, response.get("catalogAmenities").size());
      assertEquals(1, response.get("inheritedAmenities").size());
      response = data(request(put(ROOM_PATH), Map.of("amenityIds", List.of())), 200);
      assertEquals(0, response.get("catalogAmenities").size());
      assertEquals(1, response.get("inheritedAmenities").size());
      assertTrue(response.get("customAmenities").get(0).get("attached").asBoolean());
    }

    @Test
    void shouldKeepHotelCustomAmenitiesWhenLegacyPutChangesOnlyCatalog() throws Exception {
      create(HOTEL_PATH, "Suối khoáng riêng");
      request(put(HOTEL_PATH), Map.of("amenityIds", List.of(POOL)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data[0].id").value(POOL.toString()));
      var response = data(request(get(HOTEL_PATH)), 200);
      assertEquals(1, response.get("catalogAmenities").size());
      assertEquals(1, response.get("customAmenities").size());
      request(put(HOTEL_PATH), Map.of("amenityIds", List.of())).andExpect(status().isOk());
      assertEquals(1, data(request(get(HOTEL_PATH)), 200).get("customAmenities").size());
    }

    @Test
    void shouldNotReplaceExistingListWhenAnyAmenityHasWrongCategoryOrDoesNotExist()
        throws Exception {
      request(put(ROOM_PATH), Map.of("amenityIds", List.of(WIFI))).andExpect(status().isOk());
      request(put(ROOM_PATH), Map.of("amenityIds", List.of(POOL)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("AMENITY_CATEGORY_MISMATCH"));
      request(put(ROOM_PATH), Map.of("amenityIds", List.of(WIFI, UUID.randomUUID())))
          .andExpect(status().isNotFound());
      assertEquals(1, data(request(get(ROOM_PATH)), 200).get("catalogAmenities").size());
    }

    @Test
    void shouldRejectHotelAmenityAndWrongRoomHotelEvenWhenSqlBypassesApi() {
      assertThrows(
          DataIntegrityViolationException.class,
          () ->
              jdbc.update(
                  "INSERT INTO room_amenities(room_id, amenity_id) VALUES (?, ?)", ROOM, POOL));
      assertThrows(
          DataIntegrityViolationException.class,
          () ->
              jdbc.update(
                  "INSERT INTO custom_amenities(id, hotel_id, room_id, name, normalized_name, created_at, updated_at) VALUES (?, ?, ?, 'Tên', 'tên', now(), now())",
                  UUID.randomUUID(),
                  HOTEL,
                  FOREIGN_ROOM));
    }
  }

  @Nested
  @DisplayName("Phân quyền và cách ly phạm vi")
  class Authorization {
    @Test
    void shouldDenyAnonymousRequests() throws Exception {
      mvc.perform(get(HOTEL_PATH)).andExpect(status().isUnauthorized());
      mvc.perform(
              post(ROOM_PATH).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Tên\"}"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldPermitAdminAndActiveHotelManager() throws Exception {
      mvc.perform(
              post(HOTEL_PATH)
                  .with(as(ADMIN, "ADMIN"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"name\":\"Admin tạo\"}"))
          .andExpect(status().isCreated());
      mvc.perform(
              post(ROOM_PATH)
                  .with(as(MANAGER, "CUSTOMER"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"name\":\"Quản lý tạo\"}"))
          .andExpect(status().isCreated());
    }

    @Test
    void shouldDenyReceptionistAndHideHotelFromOtherOwner() throws Exception {
      mvc.perform(get(HOTEL_PATH).with(as(RECEPTION, "CUSTOMER")))
          .andExpect(status().isForbidden());
      mvc.perform(
              post(HOTEL_PATH)
                  .with(as(OTHER, "HOTEL_OWNER"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"name\":\"Không được tạo\"}"))
          .andExpect(status().isNotFound());
      mvc.perform(get(ROOM_PATH).with(as(OTHER, "HOTEL_OWNER"))).andExpect(status().isNotFound());
    }

    @Test
    void shouldHideAmenityWhenItsIdBelongsToAnotherScope() throws Exception {
      String id = create(ROOM_PATH, "Riêng phòng 101").get("id").asString();
      request(patch(HOTEL_PATH + "/" + id), Map.of("name", "Sai cấp", "expectedVersion", 0))
          .andExpect(status().isNotFound());
      request(
              patch(BASE + "/rooms/" + ROOM2 + "/amenities/" + id),
              Map.of("name", "Sai phòng", "expectedVersion", 0))
          .andExpect(status().isNotFound());
      request(
              patch(HOTEL_PATH + "/" + POOL),
              Map.of("name", "Đổi danh mục chung", "expectedVersion", 0))
          .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectPhysicalRoomBelongingToAnotherHotel() throws Exception {
      String path = BASE + "/rooms/" + FOREIGN_ROOM + "/amenities";
      request(get(path)).andExpect(status().isNotFound());
      request(post(path), Map.of("name", "Sai hotel")).andExpect(status().isNotFound());
      request(put(path), Map.of("amenityIds", List.of(WIFI))).andExpect(status().isNotFound());
    }
  }

  @Test
  @DisplayName("Hai yêu cầu tạo cùng tên không sinh hai tiện ích riêng")
  void shouldSerializeConcurrentCreatesWithSameName() throws Exception {
    try (var pool = Executors.newFixedThreadPool(2)) {
      Callable<Integer> create =
          () ->
              request(post(HOTEL_PATH), Map.of("name", "Cùng tên"))
                  .andReturn()
                  .getResponse()
                  .getStatus();
      var statuses =
          pool.invokeAll(List.of(create, create)).stream()
              .map(
                  f -> {
                    try {
                      return f.get();
                    } catch (Exception e) {
                      throw new RuntimeException(e);
                    }
                  })
              .sorted()
              .toList();
      assertEquals(List.of(201, 409), statuses);
      assertEquals(
          1,
          jdbc.queryForObject(
              "SELECT count(*) FROM custom_amenities WHERE hotel_id = ?", Integer.class, HOTEL));
    }
  }

  private JsonNode create(String path, String name) throws Exception {
    return data(request(post(path), Map.of("name", name)), 201);
  }

  private ResultActions request(MockHttpServletRequestBuilder request) throws Exception {
    return mvc.perform(request.with(as(OWNER, "HOTEL_OWNER")));
  }

  private ResultActions request(MockHttpServletRequestBuilder request, Object body)
      throws Exception {
    return request(
        request.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)));
  }

  private JsonNode data(ResultActions result, int status) throws Exception {
    return mapper
        .readTree(
            result.andExpect(status().is(status)).andReturn().getResponse().getContentAsString())
        .get("data");
  }

  private static JwtRequestPostProcessor as(UUID user, String role) {
    return jwt()
        .jwt(token -> token.subject(user.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  private void user(UUID id, String role) {
    jdbc.update(
        "INSERT INTO users(id, email, password_hash, full_name, phone, status, email_verified_at) VALUES (?, ?, 'test-hash', 'Người dùng test', ?, 'active', now()) ON CONFLICT (id) DO NOTHING",
        id,
        "scope-" + id + "@test.local",
        "+84933" + id.toString().substring(30));
    jdbc.update(
        "INSERT INTO user_roles(user_id, role_id) SELECT ?, id FROM roles WHERE role_key = ? ON CONFLICT DO NOTHING",
        id,
        role);
  }

  private void hotel(UUID id, UUID owner) {
    jdbc.update(
        "INSERT INTO hotels(id, owner_user_id, city_id, name, slug, address, status) VALUES (?, ?, ?, 'Hotel test', ?, '12 Đường Test', 'DRAFT') ON CONFLICT (id) DO UPDATE SET status = 'DRAFT'",
        id,
        owner,
        CITY,
        "scoped-amenity-" + id);
  }

  private void roomType(UUID id, UUID hotel) {
    jdbc.update(
        "INSERT INTO room_types(id, hotel_id, name, is_active) VALUES (?, ?, ?, true) ON CONFLICT (id) DO UPDATE SET is_active = true",
        id,
        hotel,
        "Scoped type " + id);
  }

  private void room(UUID id, UUID hotel, UUID type, String number) {
    jdbc.update(
        "INSERT INTO rooms(id, hotel_id, room_type_id, room_number) VALUES (?, ?, ?, ?) ON CONFLICT (id) DO NOTHING",
        id,
        hotel,
        type,
        number);
  }

  private void member(UUID user, String role) {
    jdbc.update(
        "INSERT INTO hotel_members(hotel_id, user_id, role, is_active) VALUES (?, ?, CAST(? AS hotel_member_role__t), true) ON CONFLICT (hotel_id, user_id, role) DO UPDATE SET is_active = true",
        HOTEL,
        user,
        role);
  }
}
