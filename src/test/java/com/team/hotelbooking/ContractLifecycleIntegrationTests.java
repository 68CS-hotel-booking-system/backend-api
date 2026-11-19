package com.team.hotelbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.hotelbooking.payment.ContractDirectory;
import com.team.hotelbooking.payment.ContractDirectory.ContractSnapshot;
import com.team.hotelbooking.payment.application.command.ChangeContractStatusCommand;
import com.team.hotelbooking.payment.application.usecase.ActivateContractUseCase;
import com.team.hotelbooking.payment.domain.exception.FinanceErrorCode;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
import org.springframework.transaction.support.TransactionTemplate;

// Đặt ở package gốc vì TestcontainersConfiguration là package-private.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Vòng đời hợp đồng hoa hồng trên PostgreSQL (PM-16)")
class ContractLifecycleIntegrationTests {

  private static final UUID ADMIN_ID = UUID.fromString("7c000000-0000-4000-8000-000000000001");
  private static final UUID OWNER_ID = UUID.fromString("7c000000-0000-4000-8000-000000000002");
  private static final UUID CUSTOMER_ID = UUID.fromString("7c000000-0000-4000-8000-000000000003");
  private static final UUID HOTEL_ID = UUID.fromString("7c000000-0000-4000-8000-000000000101");
  private static final UUID DA_NANG_ID = UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3");
  private static final String CONTRACTS = "/api/v1/admin/hotels/" + HOTEL_ID + "/contracts";

  @Autowired private MockMvc mvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ActivateContractUseCase activateContractUseCase;
  @Autowired private ContractDirectory contractDirectory;
  @Autowired private TransactionTemplate transactionTemplate;

  @BeforeEach
  void setUp() {
    insertUser(ADMIN_ID, "ADMIN", "+84957000001");
    insertUser(OWNER_ID, "HOTEL_OWNER", "+84957000002");
    insertUser(CUSTOMER_ID, "CUSTOMER", "+84957000003");
    jdbc.update(
        """
        INSERT INTO hotels (id, owner_user_id, city_id, name, slug, address, status)
        VALUES (?, ?, ?, 'Khách sạn hợp đồng test', 'contract-it-hotel', '3 Test', 'ACTIVE')
        ON CONFLICT (id) DO NOTHING
        """,
        HOTEL_ID,
        OWNER_ID,
        DA_NANG_ID);
    jdbc.update("DELETE FROM hotel_contracts WHERE hotel_id = ?", HOTEL_ID);
  }

  private void insertUser(UUID id, String roleKey, String phone) {
    jdbc.update(
        """
        INSERT INTO users (id, email, password_hash, full_name, phone, status, email_verified_at)
        VALUES (?, ?, 'not-a-real-hash', 'Người dùng hợp đồng', ?, 'active', now())
        ON CONFLICT (id) DO NOTHING
        """,
        id,
        "contract-it-" + id + "@test.local",
        phone);
    jdbc.update(
        """
        INSERT INTO user_roles (user_id, role_id)
        SELECT ?, id FROM roles WHERE role_key = ?
        ON CONFLICT (user_id, role_id) DO NOTHING
        """,
        id,
        roleKey);
  }

  private UUID insertContract(String code, String startsAt, String endsAt, String status) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO hotel_contracts (id, contract_code, hotel_id, owner_user_id, commission_rate,
                                     commission_basis, starts_at, ends_at, status,
                                     created_by_user_id)
        VALUES (?, ?, ?, ?, 12.50, 'BEFORE_DISCOUNT', CAST(? AS timestamptz),
                CAST(? AS timestamptz), CAST(? AS hotel_contract_status__t), ?)
        """,
        id,
        code,
        HOTEL_ID,
        OWNER_ID,
        startsAt,
        endsAt,
        status,
        ADMIN_ID);
    return id;
  }

  private static JwtRequestPostProcessor actor(UUID userId, String role) {
    return jwt()
        .jwt(token -> token.subject(userId.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  private ResultActions createContract(JwtRequestPostProcessor actor, String code, String status)
      throws Exception {
    return mvc.perform(
        post(CONTRACTS)
            .with(actor)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"contractCode": "%s", "ownerUserId": "%s", "commissionRate": 15.00,
                 "commissionBasis": "AFTER_DISCOUNT", "startsAt": "2027-01-01T00:00:00Z",
                 "endsAt": "2028-01-01T00:00:00Z", "status": "%s"}
                """
                    .formatted(code, OWNER_ID, status)));
  }

  private String statusOf(UUID contractId) {
    return jdbc.queryForObject(
        "SELECT status::text FROM hotel_contracts WHERE id = ?", String.class, contractId);
  }

  @Nested
  @DisplayName("POST /api/v1/admin/hotels/{hotelId}/contracts")
  class CreateApiTests {

    @Test
    @DisplayName("ADMIN tạo được (201), người tạo lấy từ principal")
    void shouldCreateWithAdminAsCreatorWhenActorIsAdmin() throws Exception {
      // When
      createContract(actor(ADMIN_ID, "ADMIN"), "PM16-CREATE-01", "DRAFT")
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.createdByUserId").value(ADMIN_ID.toString()))
          .andExpect(jsonPath("$.data.status").value("DRAFT"));

      // Then
      assertThat(
              jdbc.queryForObject(
                  "SELECT created_by_user_id FROM hotel_contracts WHERE contract_code = ?",
                  UUID.class,
                  "PM16-CREATE-01"))
          .isEqualTo(ADMIN_ID);
    }

    @Test
    @DisplayName("HOTEL_OWNER và CUSTOMER bị 403, không ghi hợp đồng")
    void shouldForbidNonAdminWithoutWriting() throws Exception {
      createContract(actor(OWNER_ID, "HOTEL_OWNER"), "PM16-FORBID-01", "DRAFT")
          .andExpect(status().isForbidden());
      createContract(actor(CUSTOMER_ID, "CUSTOMER"), "PM16-FORBID-02", "DRAFT")
          .andExpect(status().isForbidden());
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM hotel_contracts WHERE hotel_id = ?",
                  Integer.class,
                  HOTEL_ID))
          .isZero();
    }

    @Test
    @DisplayName("Tạo thẳng ACTIVE chồng hiệu lực với hợp đồng ACTIVE khác trả 409")
    void shouldRejectOverlappingActiveCreation() throws Exception {
      // Given
      insertContract("PM16-EXISTING", "2027-06-01T00:00:00Z", "2027-09-01T00:00:00Z", "ACTIVE");

      // When / Then
      createContract(actor(ADMIN_ID, "ADMIN"), "PM16-CREATE-02", "ACTIVE")
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("ACTIVE_CONTRACT_PERIOD_OVERLAP"));
    }
  }

  @Nested
  @DisplayName("POST .../contracts/{contractId}/activate và /terminate")
  class StatusApiTests {

    @Test
    @DisplayName("Kích hoạt, chặn hợp đồng chồng, chấm dứt rồi kích hoạt được hợp đồng kia")
    void shouldActivateTerminateAndActivateReplacement() throws Exception {
      // Given
      UUID first =
          insertContract("PM16-A", "2027-01-01T00:00:00Z", "2027-07-01T00:00:00Z", "DRAFT");
      UUID second =
          insertContract("PM16-B", "2027-03-01T00:00:00Z", "2027-12-01T00:00:00Z", "DRAFT");

      // When / Then
      mvc.perform(post(CONTRACTS + "/" + first + "/activate").with(actor(ADMIN_ID, "ADMIN")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("ACTIVE"));
      mvc.perform(post(CONTRACTS + "/" + second + "/activate").with(actor(ADMIN_ID, "ADMIN")))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("ACTIVE_CONTRACT_PERIOD_OVERLAP"));
      mvc.perform(post(CONTRACTS + "/" + first + "/activate").with(actor(ADMIN_ID, "ADMIN")))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("INVALID_CONTRACT_STATUS_TRANSITION"));
      mvc.perform(post(CONTRACTS + "/" + first + "/terminate").with(actor(OWNER_ID, "HOTEL_OWNER")))
          .andExpect(status().isForbidden());
      mvc.perform(post(CONTRACTS + "/" + first + "/terminate").with(actor(ADMIN_ID, "ADMIN")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("TERMINATED"))
          .andExpect(jsonPath("$.data.endsAt").value("2027-07-01T00:00:00Z"));
      mvc.perform(post(CONTRACTS + "/" + second + "/activate").with(actor(ADMIN_ID, "ADMIN")))
          .andExpect(status().isOk());

      // Then
      assertThat(statusOf(first)).isEqualTo("TERMINATED");
      assertThat(statusOf(second)).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Hợp đồng sau bắt đầu đúng lúc hợp đồng trước kết thúc không bị coi là chồng")
    void shouldAllowAdjacentPeriodsWhenActivating() throws Exception {
      // Given
      insertContract("PM16-PREV", "2027-01-01T00:00:00Z", "2027-06-01T00:00:00Z", "ACTIVE");
      UUID next =
          insertContract("PM16-NEXT", "2027-06-01T00:00:00Z", "2027-12-01T00:00:00Z", "DRAFT");

      // When / Then
      mvc.perform(post(CONTRACTS + "/" + next + "/activate").with(actor(ADMIN_ID, "ADMIN")))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Trả 404 khi hợp đồng thuộc khách sạn khác đường dẫn")
    void shouldReturnNotFoundWhenContractIdUnknownForHotel() throws Exception {
      mvc.perform(
              post(CONTRACTS + "/" + UUID.randomUUID() + "/activate")
                  .with(actor(ADMIN_ID, "ADMIN")))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("CONTRACT_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("Đồng thời và ràng buộc DB")
  class ConcurrencyTests {

    @Test
    @DisplayName("Hai lần kích hoạt song song hai hợp đồng chồng nhau: đúng một thành công")
    void shouldActivateExactlyOneWhenTwoOverlappingActivationsRace() throws Exception {
      // Given
      UUID first =
          insertContract("PM16-RACE-1", "2027-01-01T00:00:00Z", "2027-08-01T00:00:00Z", "DRAFT");
      UUID second =
          insertContract("PM16-RACE-2", "2027-05-01T00:00:00Z", "2027-12-01T00:00:00Z", "DRAFT");
      CyclicBarrier start = new CyclicBarrier(2);
      ExecutorService pool = Executors.newFixedThreadPool(2);
      List<Future<String>> outcomes = new ArrayList<>();

      // When
      try {
        for (UUID contractId : new UUID[] {first, second}) {
          Callable<String> task =
              () -> {
                start.await(10, TimeUnit.SECONDS);
                try {
                  activateContractUseCase.execute(
                      new ChangeContractStatusCommand(ADMIN_ID, HOTEL_ID, contractId));
                  return "ACTIVATED";
                } catch (FinanceException exception) {
                  return exception.code().name();
                }
              };
          outcomes.add(pool.submit(task));
        }
        List<String> results = new ArrayList<>();
        for (Future<String> outcome : outcomes) {
          results.add(outcome.get(30, TimeUnit.SECONDS));
        }

        // Then
        assertThat(results)
            .containsExactlyInAnyOrder(
                "ACTIVATED", FinanceErrorCode.ACTIVE_CONTRACT_PERIOD_OVERLAP.name());
        assertThat(
                jdbc.queryForObject(
                    "SELECT count(*) FROM hotel_contracts WHERE hotel_id = ? AND status = 'ACTIVE'",
                    Integer.class,
                    HOTEL_ID))
            .isEqualTo(1);
      } finally {
        pool.shutdownNow();
      }
    }

    @Test
    @DisplayName("Exclusion constraint chặn cả ghi thẳng vào DB hai hợp đồng ACTIVE chồng nhau")
    void shouldRejectOverlappingActiveRowsAtDatabaseLevel() {
      // Given
      insertContract("PM16-DB-1", "2027-01-01T00:00:00Z", "2027-08-01T00:00:00Z", "ACTIVE");

      // When / Then
      assertThrows(
          DataIntegrityViolationException.class,
          () ->
              insertContract(
                  "PM16-DB-2", "2027-07-31T00:00:00Z", "2027-12-01T00:00:00Z", "ACTIVE"));
      // DRAFT/TERMINATED chồng nhau vẫn được lưu: ràng buộc chỉ áp cho ACTIVE.
      insertContract("PM16-DB-3", "2027-02-01T00:00:00Z", "2027-03-01T00:00:00Z", "DRAFT");
      insertContract("PM16-DB-4", "2027-02-01T00:00:00Z", "2027-03-01T00:00:00Z", "TERMINATED");
    }
  }

  @Nested
  @DisplayName("ContractDirectory.findEffective")
  class DirectoryTests {

    @Test
    @DisplayName(
        "Trả snapshot hợp đồng ACTIVE đang hiệu lực; rỗng ngoài khoảng hoặc khi đã chấm dứt")
    void shouldReturnSnapshotOnlyInsideActivePeriod() {
      // Given
      UUID active =
          insertContract("PM16-DIR-1", "2027-01-01T00:00:00Z", "2027-06-01T00:00:00Z", "ACTIVE");
      insertContract("PM16-DIR-2", "2027-06-01T00:00:00Z", "2027-12-01T00:00:00Z", "TERMINATED");

      // When
      Optional<ContractSnapshot> inside =
          transactionTemplate.execute(
              status ->
                  contractDirectory.findEffective(HOTEL_ID, Instant.parse("2027-03-15T00:00:00Z")));
      Optional<ContractSnapshot> atEnd =
          transactionTemplate.execute(
              status ->
                  contractDirectory.findEffective(HOTEL_ID, Instant.parse("2027-06-01T00:00:00Z")));

      // Then
      assertThat(inside).isPresent();
      assertThat(inside.get().contractId()).isEqualTo(active);
      assertThat(inside.get().ownerUserId()).isEqualTo(OWNER_ID);
      assertThat(inside.get().commissionRate()).isEqualByComparingTo(new BigDecimal("12.50"));
      assertThat(inside.get().commissionBasis())
          .isEqualTo(ContractDirectory.CommissionBasis.BEFORE_DISCOUNT);
      assertThat(atEnd).isEmpty();
    }
  }
}
