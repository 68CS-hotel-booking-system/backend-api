package com.team.hotelbooking;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.ResultActions;

// Đặt ở package gốc vì TestcontainersConfiguration là package-private.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phân quyền tra cứu hợp đồng hoa hồng (FN01) theo hotel_financial_access")
class FinanceContractAccessIntegrationTests {

  private static final UUID OWNER_ID = UUID.fromString("99999999-dddd-4ddd-8ddd-000000000701");
  private static final UUID ADMIN_ID = UUID.fromString("99999999-dddd-4ddd-8ddd-000000000702");
  private static final UUID FINANCE_USER_ID =
      UUID.fromString("99999999-dddd-4ddd-8ddd-000000000703");
  private static final UUID REVOKED_USER_ID =
      UUID.fromString("99999999-dddd-4ddd-8ddd-000000000704");
  private static final UUID CUSTOMER_ID = UUID.fromString("99999999-dddd-4ddd-8ddd-000000000705");
  private static final UUID HOTEL_ID = UUID.fromString("99999999-eeee-4eee-8eee-000000000701");
  private static final UUID MISSING_HOTEL_ID =
      UUID.fromString("99999999-eeee-4eee-8eee-000000000799");
  private static final UUID CONTRACT_ID = UUID.fromString("99999999-ffff-4fff-8fff-000000000701");
  private static final UUID DA_NANG_ID = UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3");
  private static final String CONTRACTS_PATH = "/api/v1/finance/hotels/" + HOTEL_ID + "/contracts";

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

  private ResultActions getContracts(String uri, JwtRequestPostProcessor actor) throws Exception {
    return mvc.perform(get(uri).with(actor));
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
