package com.team.hotelbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Cập nhật hồ sơ cá nhân qua HTTP và PostgreSQL")
class UserProfileIntegrationTests {
  private static final UUID USER_ID = UUID.fromString("09f10000-0000-4000-8000-000000000001");
  private static final UUID OTHER_ID = UUID.fromString("09f10000-0000-4000-8000-000000000002");
  private static final String PATH = "/api/v1/users/me";
  @Autowired private MockMvc mvc;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void setUp() {
    insertUser(USER_ID, "+84961000001");
    insertUser(OTHER_ID, "+84961000002");
  }

  @Test
  void shouldSaveNormalizedProfileAndPreserveIdentityAndSessionWhenUpdating() throws Exception {
    // Given
    var before = jdbc.queryForMap("SELECT * FROM users WHERE id = ?", USER_ID);
    // When / Then
    update(
            """
        {"fullName":"  Nguyễn Thành Đạt  ","phone":"0961 000 003","expectedVersion":0}
        """)
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.message").value("Cập nhật hồ sơ thành công."))
        .andExpect(jsonPath("$.data.id").value(USER_ID.toString()))
        .andExpect(jsonPath("$.data.fullName").value("Nguyễn Thành Đạt"))
        .andExpect(jsonPath("$.data.phone").value("+84961000003"))
        .andExpect(jsonPath("$.data.email").value(USER_ID + "@profile.test"))
        .andExpect(jsonPath("$.data.status").value("active"))
        .andExpect(jsonPath("$.data.emailVerified").value(true))
        .andExpect(jsonPath("$.data.roles[0]").value("CUSTOMER"))
        .andExpect(jsonPath("$.data.version").value(1))
        .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    var after = jdbc.queryForMap("SELECT * FROM users WHERE id = ?", USER_ID);
    for (String field :
        List.of(
            "email",
            "password_hash",
            "status",
            "email_verified_at",
            "auth_version",
            "created_at")) {
      assertThat(after.get(field)).as(field).isEqualTo(before.get(field));
    }
    assertThat(
            jdbc.queryForObject("SELECT full_name FROM users WHERE id = ?", String.class, OTHER_ID))
        .isEqualTo("Hồ sơ ban đầu");
    mvc.perform(get(PATH).with(jwt().jwt(token -> token.subject(USER_ID.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.phone").value("+84961000003"))
        .andExpect(jsonPath("$.data.version").value(1));
    // Dùng version mới cho lần lưu kế tiếp.
    update(body("Tên tiếp theo", "+84961000003", 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.version").value(2));
  }

  @Test
  void shouldRejectStaleVersionWithoutOverwritingSavedProfile() throws Exception {
    update(body("Tên mới", "+84961000001", 0)).andExpect(status().isOk());
    update(body("Tên cũ từ tab khác", "+84961000003", 0))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("STALE_USER_VERSION"));
    assertThat(
            jdbc.queryForObject("SELECT full_name FROM users WHERE id = ?", String.class, USER_ID))
        .isEqualTo("Tên mới");
  }

  @Test
  void shouldRejectDuplicateNormalizedPhoneAndRollBackName() throws Exception {
    update(body("Không được lưu", "0961 000 002", 0))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("PHONE_ALREADY_USED"));
    assertThat(
            jdbc.queryForObject("SELECT full_name FROM users WHERE id = ?", String.class, USER_ID))
        .isEqualTo("Hồ sơ ban đầu");
  }

  @Test
  void shouldAllowUnchangedProfileWithoutIncreasingVersion() throws Exception {
    update(body("Hồ sơ ban đầu", "0961000001", 0))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.version").value(0));
  }

  @Test
  void shouldAllowOnlyOneWriterWhenTwoTabsSaveTheSameVersion() throws Exception {
    Callable<Integer> first =
        () -> update(body("Tab thứ nhất", "+84961000001", 0)).andReturn().getResponse().getStatus();
    Callable<Integer> second =
        () -> update(body("Tab thứ hai", "+84961000001", 0)).andReturn().getResponse().getStatus();
    try (var executor = Executors.newFixedThreadPool(2)) {
      var results = executor.invokeAll(List.of(first, second));
      assertThat(List.of(results.get(0).get(), results.get(1).get()))
          .containsExactlyInAnyOrder(200, 409);
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{}",
        "{\"fullName\":\" \",\"phone\":\"0961000001\",\"expectedVersion\":0}",
        "{\"fullName\":\"Tên mới\",\"phone\":\"invalid\",\"expectedVersion\":0}",
        "{\"fullName\":\"Tên mới\",\"phone\":\"0961000001\"}",
        "{\"fullName\":\"Tên mới\",\"phone\":\"0961000001\",\"expectedVersion\":-1}"
      })
  void shouldRejectInvalidProfileWithoutChangingDatabase(String body) throws Exception {
    update(body)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    assertThat(jdbc.queryForObject("SELECT version FROM users WHERE id = ?", Long.class, USER_ID))
        .isZero();
  }

  @Test
  void shouldRequireAuthentication() throws Exception {
    mvc.perform(
            put(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("Tên mới", "+84961000001", 0)))
        .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @ValueSource(strings = {"banned", "inactive", "pending"})
  void shouldRejectUnavailableAccounts(String state) throws Exception {
    jdbc.update("UPDATE users SET status = ?::user_status__t WHERE id = ?", state, USER_ID);
    update(body("Tên mới", "+84961000001", 0)).andExpect(status().isForbidden());
  }

  private ResultActions update(String body) throws Exception {
    return mvc.perform(
        put(PATH)
            .with(jwt().jwt(token -> token.subject(USER_ID.toString())))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private String body(String name, String phone, long version) {
    return """
        {"fullName":"%s","phone":"%s","expectedVersion":%d}
        """
        .formatted(name, phone, version);
  }

  private void insertUser(UUID id, String phone) {
    jdbc.update(
        """
        INSERT INTO users (id, email, password_hash, full_name, phone, status, email_verified_at, auth_version, version)
        VALUES (?, ?, 'preserved-password-hash', 'Hồ sơ ban đầu', ?, 'active', now(), 3, 0)
        ON CONFLICT (id) DO UPDATE SET full_name = 'Hồ sơ ban đầu', phone = EXCLUDED.phone,
          status = 'active', email_verified_at = now(), auth_version = 3, version = 0
        """,
        id,
        id + "@profile.test",
        phone);
    jdbc.update(
        """
        INSERT INTO user_roles (user_id, role_id)
        SELECT ?, id FROM roles WHERE role_key = 'CUSTOMER'
        ON CONFLICT (user_id, role_id) DO NOTHING
        """,
        id);
  }
}
