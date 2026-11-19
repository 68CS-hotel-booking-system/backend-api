package com.team.hotelbooking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
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
@DisplayName("Danh mục tiện ích public")
class AmenityCatalogIntegrationTests {

  private static final String AMENITIES_PATH = "/api/v1/amenities";
  private static final Set<String> HOTEL_CODES =
      Set.of(
          "FREE_PARKING",
          "SWIMMING_POOL",
          "RESTAURANT",
          "FITNESS_CENTER",
          "RECEPTION_24H",
          "AIRPORT_SHUTTLE");
  private static final Set<String> ROOM_CODES =
      Set.of(
          "FREE_WIFI", "AIR_CONDITIONING", "TELEVISION", "MINIBAR", "PRIVATE_BATHROOM", "BALCONY");

  @Autowired private MockMvc mvc;

  @Autowired private ObjectMapper mapper;

  @Autowired private JdbcTemplate jdbc;

  @Nested
  @DisplayName("GET /api/v1/amenities")
  class ListAmenitiesApiTests {

    @Test
    void shouldReturnSeededAmenitiesInDatabaseOrderWhenRequestHasNoToken() throws Exception {
      // Given
      // Thứ tự name phụ thuộc collation của DB test, nên lấy kỳ vọng từ chính DB đó.
      List<String> expectedCodes =
          jdbc.queryForList("SELECT code FROM amenities ORDER BY category, name, id", String.class);

      // When
      JsonNode data = readData(AMENITIES_PATH);

      // Then
      assertEquals(expectedCodes, codes(data));
      assertEquals(
          union(HOTEL_CODES, ROOM_CODES), Set.copyOf(codes(data)), "Phải có đủ 12 tiện ích seed");
    }

    @Test
    void shouldReturnOnlyHotelAmenitiesWhenCategoryIsHotel() throws Exception {
      // Given
      List<String> expectedCodes =
          jdbc.queryForList(
              "SELECT code FROM amenities WHERE category = 'HOTEL' ORDER BY name, id",
              String.class);

      // When
      JsonNode data = readData(AMENITIES_PATH + "?category=HOTEL");

      // Then
      assertEquals(expectedCodes, codes(data));
      assertEquals(HOTEL_CODES, Set.copyOf(codes(data)));
      data.valueStream().forEach(item -> assertEquals("HOTEL", item.get("category").asString()));
    }

    @Test
    void shouldReturnOnlyRoomAmenitiesWhenCategoryIsRoom() throws Exception {
      // When
      JsonNode data = readData(AMENITIES_PATH + "?category=ROOM");

      // Then
      assertEquals(ROOM_CODES, Set.copyOf(codes(data)));
      data.valueStream().forEach(item -> assertEquals("ROOM", item.get("category").asString()));
    }

    @Test
    void shouldRejectRequestWhenCategoryIsLowercase() throws Exception {
      // When / Then
      mvc.perform(get(AMENITIES_PATH).param("category", "hotel"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void shouldRejectRequestWhenBearerTokenIsInvalid() throws Exception {
      // When / Then
      mvc.perform(get(AMENITIES_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("POST /api/v1/amenities")
  class CreateAmenityApiTests {

    @Test
    void shouldRejectRequestWhenNoTokenIsProvided() throws Exception {
      // When / Then
      mvc.perform(post(AMENITIES_PATH)).andExpect(status().isUnauthorized());
    }
  }

  private JsonNode readData(String uri) throws Exception {
    String body =
        mvc.perform(get(uri))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return mapper.readTree(body).get("data");
  }

  private static List<String> codes(JsonNode data) {
    return data.valueStream().map(item -> item.get("code").asString()).toList();
  }

  private static Set<String> union(Set<String> first, Set<String> second) {
    return java.util.stream.Stream.concat(first.stream(), second.stream())
        .collect(java.util.stream.Collectors.toSet());
  }
}
