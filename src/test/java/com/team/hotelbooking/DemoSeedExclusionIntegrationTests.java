package com.team.hotelbooking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

// Đặt ở package gốc vì TestcontainersConfiguration là package-private.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Dữ liệu demo chỉ có ở profile dev")
class DemoSeedExclusionIntegrationTests {

  @Autowired private JdbcTemplate jdbc;

  @Nested
  @DisplayName("Flyway ở profile test")
  class FlywayTests {

    @Test
    @DisplayName("Không áp repeatable migration seed demo")
    void shouldNotApplyDemoSeedMigrationWhenProfileIsTest() {
      assertEquals(
          0, count("SELECT count(*) FROM flyway_schema_history WHERE script LIKE '%seed_demo%'"));
    }

    @Test
    @DisplayName("Không có owner, khách sạn hay loại phòng demo")
    void shouldHaveNoDemoRowsWhenProfileIsTest() {
      assertEquals(
          0, count("SELECT count(*) FROM users WHERE email LIKE 'owner.demo%@example.com'"));
      assertEquals(0, count("SELECT count(*) FROM hotels WHERE slug LIKE 'demo-%'"));
      assertEquals(0, count("SELECT count(*) FROM room_types WHERE id::text LIKE 'de0000c0-%'"));
    }
  }

  private int count(String sql) {
    Integer value = jdbc.queryForObject(sql, Integer.class);
    return value == null ? 0 : value;
  }
}
