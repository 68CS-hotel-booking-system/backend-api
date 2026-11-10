package com.team.hotelbooking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
@DisplayName("Phân quyền tra cứu và đặt giá phòng (PR01/PR02) qua HotelDirectory")
class ManagementRateAccessIntegrationTests {

  private static final UUID OWNER_ID = UUID.fromString("99999999-aaaa-4aaa-8aaa-000000000601");
  private static final UUID ADMIN_ID = UUID.fromString("99999999-aaaa-4aaa-8aaa-000000000602");
  private static final UUID MANAGER_ID = UUID.fromString("99999999-aaaa-4aaa-8aaa-000000000603");
  private static final UUID OTHER_MANAGER_ID =
      UUID.fromString("99999999-aaaa-4aaa-8aaa-000000000604");
  private static final UUID RECEPTIONIST_ID =
      UUID.fromString("99999999-aaaa-4aaa-8aaa-000000000605");
  private static final UUID CUSTOMER_ID = UUID.fromString("99999999-aaaa-4aaa-8aaa-000000000606");
  private static final UUID HOTEL_ID = UUID.fromString("99999999-bbbb-4bbb-8bbb-000000000601");
  private static final UUID OTHER_HOTEL_ID =
      UUID.fromString("99999999-bbbb-4bbb-8bbb-000000000602");
  private static final UUID ROOM_TYPE_ID = UUID.fromString("99999999-cccc-4ccc-8ccc-000000000601");
  private static final UUID DA_NANG_ID = UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3");
  // Ngày xa trong tương lai để PUT không bị chặn bởi quy tắc "không đặt giá quá khứ".
  private static final LocalDate RATE_DATE = LocalDate.of(2027, 6, 1);
  private static final String RATES_PATH =
      "/api/v1/management/hotels/" + HOTEL_ID + "/room-types/" + ROOM_TYPE_ID + "/rates";
  private static final String PUT_BODY = "{\"startDate\": \"2027-06-01\", \"amount\": 1500000}";

  @Autowired private MockMvc mvc;

  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void setUp() {
    // Fixture riêng của lớp này; xóa giá đã đặt để mỗi test bắt đầu từ trạng thái chưa có giá.
    insertUser(OWNER_ID, "HOTEL_OWNER");
    insertUser(ADMIN_ID, "ADMIN");
    insertUser(MANAGER_ID, "CUSTOMER");
    insertUser(OTHER_MANAGER_ID, "CUSTOMER");
    insertUser(RECEPTIONIST_ID, "CUSTOMER");
    insertUser(CUSTOMER_ID, "CUSTOMER");
    insertActiveHotel(HOTEL_ID);
    insertActiveHotel(OTHER_HOTEL_ID);
    jdbc.update("UPDATE hotels SET status = 'ACTIVE' WHERE id = ?", HOTEL_ID);
    jdbc.update(
        """
        INSERT INTO room_types (id, hotel_id, name, is_active)
        VALUES (?, ?, 'Phòng giá test', true)
        ON CONFLICT (id) DO NOTHING
        """,
        ROOM_TYPE_ID,
        HOTEL_ID);
    jdbc.update("DELETE FROM room_rates WHERE room_type_id = ?", ROOM_TYPE_ID);
    insertMember(HOTEL_ID, MANAGER_ID, "HOTEL_MANAGER");
    insertMember(OTHER_HOTEL_ID, OTHER_MANAGER_ID, "HOTEL_MANAGER");
    insertMember(HOTEL_ID, RECEPTIONIST_ID, "RECEPTIONIST");
  }

  @Nested
  @DisplayName("GET /api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}/rates")
  class GetRoomRatesApiTests {

    @Test
    void shouldReturnRatesWhenActorIsOwnerAdminOrHotelManager() throws Exception {
      // When / Then
      for (JwtRequestPostProcessor allowed :
          new JwtRequestPostProcessor[] {
            actor(OWNER_ID, "HOTEL_OWNER"), actor(ADMIN_ID, "ADMIN"), actor(MANAGER_ID, "CUSTOMER")
          }) {
        getRates(allowed)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roomTypeId").value(ROOM_TYPE_ID.toString()));
      }
    }

    @Test
    void shouldReturnForbiddenWhenActorIsReceptionist() throws Exception {
      // When / Then
      getRates(actor(RECEPTIONIST_ID, "CUSTOMER"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void shouldReturnNotFoundWhenActorIsCustomerOrManagesAnotherHotel() throws Exception {
      // When / Then
      getRates(actor(CUSTOMER_ID, "CUSTOMER"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
      getRates(actor(OTHER_MANAGER_ID, "CUSTOMER"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("PUT /api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}/rates")
  class SetRoomRatesApiTests {

    @Test
    void shouldStoreRateWhenActorIsHotelManager() throws Exception {
      // When
      putRates(actor(MANAGER_ID, "CUSTOMER")).andExpect(status().isOk());

      // Then
      assertEquals(
          1_500_000L,
          jdbc.queryForObject(
              "SELECT amount FROM room_rates WHERE room_type_id = ? AND rate_date = ?",
              Long.class,
              ROOM_TYPE_ID,
              RATE_DATE));
    }

    @Test
    void shouldReturnForbiddenWithoutStoringRateWhenActorIsReceptionist() throws Exception {
      // When
      putRates(actor(RECEPTIONIST_ID, "CUSTOMER"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

      // Then
      assertEquals(0, countRates());
    }

    @Test
    void shouldReturnNotFoundWithoutStoringRateWhenActorIsCustomer() throws Exception {
      // When
      putRates(actor(CUSTOMER_ID, "CUSTOMER"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));

      // Then
      assertEquals(0, countRates());
    }
  }

  @Nested
  @DisplayName("Trạng thái khách sạn khi quản lý giá phòng")
  class HotelStatusTests {

    @Test
    @DisplayName("Owner đặt và tra giá được khi khách sạn còn DRAFT hoặc đang SUSPENDED")
    void shouldAllowRatesWhenHotelIsDraftOrSuspended() throws Exception {
      for (String hotelStatus : new String[] {"DRAFT", "SUSPENDED"}) {
        // Given
        setHotelStatus(hotelStatus);

        // When / Then
        putRates(actor(OWNER_ID, "HOTEL_OWNER")).andExpect(status().isOk());
        getRates(actor(OWNER_ID, "HOTEL_OWNER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.rates[0].price").value(1_500_000));
      }
    }

    @Test
    @DisplayName("Trả 409 HOTEL_LOCKED và không lưu giá khi khách sạn bị khóa, kể cả ADMIN")
    void shouldReturnConflictWithoutStoringRateWhenHotelIsLocked() throws Exception {
      // Given
      setHotelStatus("LOCKED");

      // When / Then
      putRates(actor(ADMIN_ID, "ADMIN"))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("HOTEL_LOCKED"));
      getRates(actor(OWNER_ID, "HOTEL_OWNER"))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("HOTEL_LOCKED"));
      assertEquals(0, countRates());
    }

    @Test
    @DisplayName("Người ngoài vẫn nhận 404 với khách sạn bị khóa, không lộ trạng thái")
    void shouldReturnNotFoundForOutsiderWhenHotelIsLocked() throws Exception {
      // Given
      setHotelStatus("LOCKED");

      // When / Then
      getRates(actor(CUSTOMER_ID, "CUSTOMER"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
    }
  }

  private void setHotelStatus(String hotelStatus) {
    jdbc.update(
        "UPDATE hotels SET status = CAST(? AS hotel_status__t) WHERE id = ?",
        hotelStatus,
        HOTEL_ID);
  }

  private ResultActions getRates(JwtRequestPostProcessor actor) throws Exception {
    return mvc.perform(
        get(RATES_PATH)
            .with(actor)
            .param("startDate", "2027-06-01")
            .param("endDate", "2027-06-02"));
  }

  private ResultActions putRates(JwtRequestPostProcessor actor) throws Exception {
    return mvc.perform(
        put(RATES_PATH).with(actor).contentType(MediaType.APPLICATION_JSON).content(PUT_BODY));
  }

  private int countRates() {
    Integer count =
        jdbc.queryForObject(
            "SELECT count(*) FROM room_rates WHERE room_type_id = ?", Integer.class, ROOM_TYPE_ID);
    return count == null ? 0 : count;
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
        "rate-access-" + id + "@test.local",
        // phone UNIQUE: dải +84944... riêng của lớp này, lấy 6 số cuối của id.
        "+84944" + id.toString().substring(30));
    jdbc.update(
        """
        INSERT INTO user_roles (user_id, role_id)
        SELECT ?, id FROM roles WHERE role_key = ?
        ON CONFLICT (user_id, role_id) DO NOTHING
        """,
        id,
        roleKey);
  }

  private void insertActiveHotel(UUID id) {
    jdbc.update(
        """
        INSERT INTO hotels (id, owner_user_id, city_id, name, slug, address, status)
        VALUES (?, ?, ?, 'Khách sạn giá test', ?, '7 Lê Duẩn', 'ACTIVE')
        ON CONFLICT (id) DO NOTHING
        """,
        id,
        OWNER_ID,
        DA_NANG_ID,
        "rate-access-" + id);
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
