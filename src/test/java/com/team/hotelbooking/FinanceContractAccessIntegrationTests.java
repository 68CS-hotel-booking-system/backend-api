package com.team.hotelbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
@DisplayName(
    "Phân quyền tài chính: ADMIN cấp/thu hồi (FN00, FN16) và tra cứu hợp đồng hoa hồng (FN01)")
class FinanceContractAccessIntegrationTests {

  private static final UUID OWNER_ID = UUID.fromString("99999999-dddd-4ddd-8ddd-000000000701");
  private static final UUID ADMIN_ID = UUID.fromString("99999999-dddd-4ddd-8ddd-000000000702");
  private static final UUID FINANCE_USER_ID =
      UUID.fromString("99999999-dddd-4ddd-8ddd-000000000703");
  private static final UUID REVOKED_USER_ID =
      UUID.fromString("99999999-dddd-4ddd-8ddd-000000000704");
  private static final UUID CUSTOMER_ID = UUID.fromString("99999999-dddd-4ddd-8ddd-000000000705");
  // Người nhận quyền trong các test PUT; bản ghi quyền được xóa lại ở mỗi setUp.
  private static final UUID GRANTEE_ID = UUID.fromString("99999999-dddd-4ddd-8ddd-000000000706");
  private static final UUID HOTEL_ID = UUID.fromString("99999999-eeee-4eee-8eee-000000000701");
  private static final UUID MISSING_HOTEL_ID =
      UUID.fromString("99999999-eeee-4eee-8eee-000000000799");
  private static final UUID CONTRACT_ID = UUID.fromString("99999999-ffff-4fff-8fff-000000000701");
  private static final UUID DA_NANG_ID = UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3");
  private static final String CONTRACTS_PATH = "/api/v1/finance/hotels/" + HOTEL_ID + "/contracts";
  private static final String ACCESS_PATH =
      "/api/v1/admin/hotels/" + HOTEL_ID + "/financial-access/{userId}";

  @Autowired private MockMvc mvc;

  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void setUp() {
    // Fixture riêng của lớp này; chạy lại nhiều lần nhờ ON CONFLICT.
    insertUser(OWNER_ID, "HOTEL_OWNER");
    insertUser(ADMIN_ID, "ADMIN");
    insertUser(FINANCE_USER_ID, "CUSTOMER");
    insertUser(REVOKED_USER_ID, "CUSTOMER");
    insertUser(CUSTOMER_ID, "CUSTOMER");
    insertUser(GRANTEE_ID, "CUSTOMER");
    // Lịch sử xóa theo ON DELETE CASCADE.
    jdbc.update(
        "DELETE FROM hotel_financial_access WHERE hotel_id = ? AND user_id IN (?, ?)",
        HOTEL_ID,
        GRANTEE_ID,
        CUSTOMER_ID);
    jdbc.update(
        """
        INSERT INTO hotels (id, owner_user_id, city_id, name, slug, address)
        VALUES (?, ?, ?, 'Khách sạn tài chính test', ?, '9 Hùng Vương')
        ON CONFLICT (id) DO NOTHING
        """,
        HOTEL_ID,
        OWNER_ID,
        DA_NANG_ID,
        "finance-access-" + HOTEL_ID);
    jdbc.update(
        """
        INSERT INTO hotel_contracts (id, contract_code, hotel_id, owner_user_id, commission_rate,
                                     commission_basis, starts_at, ends_at, status,
                                     created_by_user_id)
        VALUES (?, 'FIN-ACCESS-701', ?, ?, 12.50, 'BEFORE_DISCOUNT',
                '2026-01-01T00:00:00Z', '2027-01-01T00:00:00Z', 'ACTIVE', ?)
        ON CONFLICT (id) DO NOTHING
        """,
        CONTRACT_ID,
        HOTEL_ID,
        OWNER_ID,
        ADMIN_ID);
    grantFinancialAccess(FINANCE_USER_ID, true);
    grantFinancialAccess(REVOKED_USER_ID, false);
  }

  @Nested
  @DisplayName("GET /api/v1/finance/hotels/{hotelId}/contracts")
  class GetContractsApiTests {

    @Test
    void shouldReturnContractsWhenActorIsAdmin() throws Exception {
      // When / Then
      getContracts(CONTRACTS_PATH, actor(ADMIN_ID, "ADMIN"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content[0].contractCode").value("FIN-ACCESS-701"))
          .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void shouldReturnContractsWhenActorIsHotelOwner() throws Exception {
      // When / Then
      getContracts(CONTRACTS_PATH, actor(OWNER_ID, "HOTEL_OWNER"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content[0].id").value(CONTRACT_ID.toString()));
    }

    @Test
    void shouldReturnContractsWhenActorHasGrantedFinancialAccess() throws Exception {
      // When / Then
      getContracts(CONTRACTS_PATH, actor(FINANCE_USER_ID, "CUSTOMER"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content[0].id").value(CONTRACT_ID.toString()));
    }

    @Test
    void shouldReturnForbiddenWhenActorIsCustomerWithoutFinancialAccess() throws Exception {
      // When / Then
      getContracts(CONTRACTS_PATH, actor(CUSTOMER_ID, "CUSTOMER"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void shouldReturnForbiddenWhenFinancialAccessIsRevoked() throws Exception {
      // When / Then
      getContracts(CONTRACTS_PATH, actor(REVOKED_USER_ID, "CUSTOMER"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void shouldReturnNotFoundWhenHotelDoesNotExist() throws Exception {
      // When / Then
      getContracts(
              "/api/v1/finance/hotels/" + MISSING_HOTEL_ID + "/contracts", actor(ADMIN_ID, "ADMIN"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("PUT /api/v1/admin/hotels/{hotelId}/financial-access/{userId}")
  class UpdateFinancialAccessApiTests {

    @Test
    void shouldReturnUnauthorizedWhenRequestHasNoToken() throws Exception {
      // When / Then
      mvc.perform(
              put(ACCESS_PATH, GRANTEE_ID)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(grantBody(true, null)))
          .andExpect(status().isUnauthorized());
      assertThat(accessRowCount(GRANTEE_ID)).isZero();
    }

    @Test
    void shouldReturnForbiddenWhenCustomerGrantsAccessToAnotherUser() throws Exception {
      // When / Then
      putAccess(GRANTEE_ID, grantBody(true, null), actor(CUSTOMER_ID, "CUSTOMER"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
      assertThat(accessRowCount(GRANTEE_ID)).isZero();
    }

    @Test
    void shouldReturnForbiddenWhenHotelOwnerGrantsAccess() throws Exception {
      // When / Then
      putAccess(GRANTEE_ID, grantBody(true, null), actor(OWNER_ID, "HOTEL_OWNER"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
      assertThat(accessRowCount(GRANTEE_ID)).isZero();
    }

    @Test
    void shouldNotLetCustomerSelfGrantAccessAndReadContracts() throws Exception {
      // When: khách tự cấp quyền tài chính cho chính mình
      putAccess(CUSTOMER_ID, grantBody(true, null), actor(CUSTOMER_ID, "CUSTOMER"))
          // Then
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
      assertThat(accessRowCount(CUSTOMER_ID)).isZero();

      // When / Then: vẫn không đọc được hợp đồng hoa hồng
      getContracts(CONTRACTS_PATH, actor(CUSTOMER_ID, "CUSTOMER"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void shouldRecordAdminAsActorWhenAdminGrantsThenRevokesAccess() throws Exception {
      // When: ADMIN cấp quyền lần đầu
      putAccess(GRANTEE_ID, grantBody(true, null), actor(ADMIN_ID, "ADMIN"))
          // Then
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.userId").value(GRANTEE_ID.toString()))
          .andExpect(jsonPath("$.data.isGranted").value(true))
          .andExpect(jsonPath("$.data.grantedByUserId").value(ADMIN_ID.toString()));
      assertThat(grantedBy(GRANTEE_ID)).isEqualTo(ADMIN_ID);
      assertThat(historyActors(GRANTEE_ID)).containsExactly(ADMIN_ID);

      // When / Then: người được cấp đọc được hợp đồng
      getContracts(CONTRACTS_PATH, actor(GRANTEE_ID, "CUSTOMER"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content[0].id").value(CONTRACT_ID.toString()));

      // When: ADMIN thu hồi với version hiện tại
      putAccess(GRANTEE_ID, grantBody(false, accessVersion(GRANTEE_ID)), actor(ADMIN_ID, "ADMIN"))
          // Then
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.isGranted").value(false))
          .andExpect(jsonPath("$.data.grantedByUserId").value(ADMIN_ID.toString()));
      assertThat(grantedBy(GRANTEE_ID)).isEqualTo(ADMIN_ID);
      assertThat(historyActors(GRANTEE_ID)).containsExactly(ADMIN_ID, ADMIN_ID);

      // When / Then: sau khi thu hồi không đọc được nữa
      getContracts(CONTRACTS_PATH, actor(GRANTEE_ID, "CUSTOMER"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
  }

  @Nested
  @DisplayName("GET /api/v1/admin/hotels/{hotelId}/financial-access/{userId}")
  class GetFinancialAccessApiTests {

    @Test
    void shouldReturnForbiddenWhenCustomerReadsAccessDetail() throws Exception {
      // When / Then
      mvc.perform(get(ACCESS_PATH, FINANCE_USER_ID).with(actor(CUSTOMER_ID, "CUSTOMER")))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
          .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void shouldReturnForbiddenWhenGrantedFinanceUserReadsAccessDetail() throws Exception {
      // When / Then
      mvc.perform(get(ACCESS_PATH, FINANCE_USER_ID).with(actor(FINANCE_USER_ID, "CUSTOMER")))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void shouldReturnAccessDetailWhenActorIsAdmin() throws Exception {
      // When / Then
      mvc.perform(get(ACCESS_PATH, FINANCE_USER_ID).with(actor(ADMIN_ID, "ADMIN")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.userId").value(FINANCE_USER_ID.toString()))
          .andExpect(jsonPath("$.data.isGranted").value(true));
    }
  }

  private ResultActions getContracts(String uri, JwtRequestPostProcessor actor) throws Exception {
    return mvc.perform(get(uri).with(actor));
  }

  private ResultActions putAccess(UUID userId, String body, JwtRequestPostProcessor actor)
      throws Exception {
    return mvc.perform(
        put(ACCESS_PATH, userId).with(actor).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private static String grantBody(boolean granted, Long expectedVersion) {
    return """
        {"isGranted": %s, "accessScope": "FINANCE_READ", "reason": "Kiểm thử phân quyền",
         "expectedVersion": %s}
        """
        .formatted(granted, expectedVersion);
  }

  private int accessRowCount(UUID userId) {
    Integer count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM hotel_financial_access WHERE hotel_id = ? AND user_id = ?",
            Integer.class,
            HOTEL_ID,
            userId);
    return count == null ? 0 : count;
  }

  private UUID grantedBy(UUID userId) {
    return jdbc.queryForObject(
        "SELECT granted_by_user_id FROM hotel_financial_access WHERE hotel_id = ? AND user_id = ?",
        UUID.class,
        HOTEL_ID,
        userId);
  }

  private Long accessVersion(UUID userId) {
    return jdbc.queryForObject(
        "SELECT version FROM hotel_financial_access WHERE hotel_id = ? AND user_id = ?",
        Long.class,
        HOTEL_ID,
        userId);
  }

  private List<UUID> historyActors(UUID userId) {
    return jdbc.queryForList(
        """
        SELECT changed_by_user_id FROM hotel_financial_access_history
        WHERE hotel_id = ? AND user_id = ?
        ORDER BY version, changed_at
        """,
        UUID.class,
        HOTEL_ID,
        userId);
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
        "finance-access-" + id + "@test.local",
        // phone UNIQUE: dải +84955... riêng của lớp này, lấy 6 số cuối của id.
        "+84955" + id.toString().substring(30));
    jdbc.update(
        """
        INSERT INTO user_roles (user_id, role_id)
        SELECT ?, id FROM roles WHERE role_key = ?
        ON CONFLICT (user_id, role_id) DO NOTHING
        """,
        id,
        roleKey);
  }

  private void grantFinancialAccess(UUID userId, boolean granted) {
    jdbc.update(
        """
        INSERT INTO hotel_financial_access (hotel_id, user_id, is_granted, granted_by_user_id)
        VALUES (?, ?, ?, ?)
        ON CONFLICT (hotel_id, user_id) DO UPDATE SET is_granted = EXCLUDED.is_granted
        """,
        HOTEL_ID,
        userId,
        granted,
        ADMIN_ID);
  }
}
