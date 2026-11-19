package com.team.hotelbooking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
@DisplayName("Danh mục thành phố public")
class CityCatalogIntegrationTests {

  private static final String CITIES_PATH = "/api/v1/cities";
  private static final List<String> SEEDED_SLUGS =
      List.of("ha-noi", "ho-chi-minh", "da-nang", "hai-phong", "can-tho", "hue");

  @Autowired private MockMvc mvc;

  @Autowired private ObjectMapper mapper;

  @Autowired private JdbcTemplate jdbc;

  @Nested
  @DisplayName("GET /api/v1/cities")
  class ListCitiesApiTests {

    @Test
    void shouldReturnSeededCitiesInDatabaseOrderWhenRequestHasNoToken() throws Exception {
      // Given
      // Thứ tự name phụ thuộc collation của DB test, nên lấy kỳ vọng từ chính DB đó.
      List<String> expectedSlugs =
          jdbc.queryForList(
              "SELECT slug FROM cities ORDER BY country_code, name, id", String.class);

      // When
      String body =
          mvc.perform(get(CITIES_PATH))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      // Then
      JsonNode data = mapper.readTree(body).get("data");
      List<String> slugs = data.valueStream().map(city -> city.get("slug").asString()).toList();
      List<String> countryCodes =
          data.valueStream().map(city -> city.get("countryCode").asString()).toList();

      assertTrue(slugs.containsAll(SEEDED_SLUGS));
      assertEquals(expectedSlugs, slugs);
      for (int i = 1; i < countryCodes.size(); i++) {
        assertTrue(countryCodes.get(i - 1).compareTo(countryCodes.get(i)) <= 0);
      }
    }

    @Test
    void shouldRejectRequestWhenBearerTokenIsInvalid() throws Exception {
      // When / Then
      mvc.perform(get(CITIES_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("POST /api/v1/cities")
  class CreateCityApiTests {

    @Test
    void shouldRejectRequestWhenNoTokenIsProvided() throws Exception {
      // When / Then
      mvc.perform(post(CITIES_PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
          .andExpect(status().isUnauthorized());
    }
  }
}
