package com.team.hotelbooking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// Đặt ở package gốc vì TestcontainersConfiguration là package-private.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Chi tiết khách sạn công khai")
class PublicHotelIntegrationTests {

  private static final UUID OWNER_ID = UUID.fromString("0d5c6a7b-8e9f-4a1b-9c2d-3e4f5a6b7c8d");
  private static final UUID ACTIVE_HOTEL_ID =
      UUID.fromString("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d");
  private static final UUID DRAFT_HOTEL_ID =
      UUID.fromString("b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e");
  private static final UUID MISSING_HOTEL_ID =
      UUID.fromString("c3d4e5f6-a7b8-4c9d-8e1f-2a3b4c5d6e7f");
  private static final UUID DA_NANG_ID = UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3");

  @Autowired private MockMvc mvc;

  @Autowired private ObjectMapper mapper;

  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void setUp() {
    // Fixture chỉ dành cho test; không đưa dữ liệu demo vào migration chạy trên mọi môi trường.
    jdbc.update(
        """
        INSERT INTO users (id, email, password_hash, full_name, phone, status)
        VALUES (?, 'public-hotel-owner@test.local', 'not-a-real-hash', 'Chủ khách sạn', '+84900000001',
                'active')
        ON CONFLICT (id) DO NOTHING
        """,
        OWNER_ID);
    insertHotel(ACTIVE_HOTEL_ID, "Khách sạn Biển Xanh", "ACTIVE");
    insertHotel(DRAFT_HOTEL_ID, "Khách sạn Nháp", "DRAFT");
  }

  @Nested
  @DisplayName("GET /api/v1/hotels/{hotelId}")
  class GetPublicHotelApiTests {

    @Test
    void shouldReturnPublicFieldsWhenHotelIsActiveAndRequestHasNoToken() throws Exception {
      // When
      String body =
          mvc.perform(get("/api/v1/hotels/" + ACTIVE_HOTEL_ID))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.message").value("Lấy thông tin khách sạn thành công."))
              .andExpect(jsonPath("$.data.id").value(ACTIVE_HOTEL_ID.toString()))
              .andExpect(jsonPath("$.data.cityId").value(DA_NANG_ID.toString()))
              .andExpect(jsonPath("$.data.name").value("Khách sạn Biển Xanh"))
              .andExpect(jsonPath("$.data.checkInTime").value("14:00:00"))
              .andReturn()
              .getResponse()
              .getContentAsString();

      // Then
      JsonNode data = mapper.readTree(body).get("data");
      List<String> fields = data.propertyNames().stream().toList();
      assertEquals(
          List.of(
              "id",
              "cityId",
              "name",
              "slug",
              "description",
              "address",
              "latitude",
              "longitude",
              "starRating",
              "checkInTime",
              "checkOutTime"),
          fields);
      assertFalse(body.contains(OWNER_ID.toString()), "Không được lộ Owner");
      assertFalse(body.contains("ACTIVE"), "Không được lộ trạng thái");
    }

    @Test
    void shouldReturnSameNotFoundWhenHotelIsDraftOrMissing() throws Exception {
      // When
      JsonNode draft = readProblem(DRAFT_HOTEL_ID);
      JsonNode missing = readProblem(MISSING_HOTEL_ID);

      // Then
      assertEquals(404, draft.get("status").asInt());
      assertEquals("HOTEL_NOT_FOUND", draft.get("code").asString());
      for (String field : List.of("status", "title", "code", "detail", "errors")) {
        assertEquals(missing.get(field), draft.get(field), "Field khác nhau: " + field);
      }
    }

    @Test
    void shouldRejectRequestWhenHotelIdIsNotUuid() throws Exception {
      // When / Then
      mvc.perform(get("/api/v1/hotels/not-a-uuid"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldRejectRequestWhenBearerTokenIsInvalid() throws Exception {
      // When / Then
      mvc.perform(
              get("/api/v1/hotels/" + ACTIVE_HOTEL_ID)
                  .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("POST /api/v1/hotels/{hotelId}")
  class PostPublicHotelApiTests {

    @Test
    void shouldRejectRequestWhenNoTokenIsProvided() throws Exception {
      // When / Then
      mvc.perform(post("/api/v1/hotels/" + ACTIVE_HOTEL_ID)).andExpect(status().isUnauthorized());
    }
  }

  private void insertHotel(UUID id, String name, String status) {
    jdbc.update(
        """
        INSERT INTO hotels (id, owner_user_id, city_id, name, slug, address, status)
        VALUES (?, ?, ?, ?, ?, '12 Võ Nguyên Giáp', CAST(? AS hotel_status__t))
        ON CONFLICT (id) DO NOTHING
        """,
        id,
        OWNER_ID,
        DA_NANG_ID,
        name,
        "test-" + id,
        status);
  }

  private JsonNode readProblem(UUID hotelId) throws Exception {
    return readProblemAt("/api/v1/hotels/" + hotelId);
  }

  private JsonNode readProblemAt(String uri) throws Exception {
    String body =
        mvc.perform(get(uri))
            .andExpect(status().isNotFound())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return mapper.readTree(body);
  }
}
