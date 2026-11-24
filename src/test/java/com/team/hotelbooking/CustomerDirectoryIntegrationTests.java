package com.team.hotelbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Khách hàng theo quan hệ nghiệp vụ của admin và owner")
class CustomerDirectoryIntegrationTests {
  private static final Instant NOW = Instant.parse("2026-11-24T00:00:00Z");
  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;
  @Autowired TransactionTemplate transaction;
  @MockitoBean Clock clock;
  String prefix;
  UUID admin, ownerA, ownerB, customerX, customerY, hotelA, hotelA2, hotelB;

  @BeforeEach
  void setUp() {
    given(clock.instant()).willReturn(NOW);
    prefix = "CD-" + UUID.randomUUID().toString().substring(0, 8);
    admin = user("ADMIN", "Admin");
    ownerA = user("HOTEL_OWNER", "A Owner");
    ownerB = user("HOTEL_OWNER", "B Owner");
    customerX = user("CUSTOMER", "X Khách");
    customerY = user("CUSTOMER", "Y Khách");
    hotelA = hotel(ownerA);
    hotelA2 = hotel(ownerA);
    hotelB = hotel(ownerB);
  }

  @Test
  void shouldScopeContactsAndStatisticsToHotelsOwnedByTheActor() throws Exception {
    booking(customerX, hotelA, "Khách cũ", "CHECKED_OUT", NOW.minusSeconds(200));
    booking(customerX, hotelA2, "Khách tại A", "PENDING_PAYMENT", NOW.minusSeconds(100));
    booking(customerX, hotelB, "THÔNG TIN CHỈ OWNER B THẤY", "CONFIRMED", NOW);
    booking(customerY, hotelB, "Khách riêng B", "CANCELLED", NOW);
    list("/management/customers", ownerA, "HOTEL_OWNER", "ownerId=" + ownerB + "&hotelId=" + hotelB)
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].id").value(customerX.toString()))
        .andExpect(jsonPath("$.data.content[0].fullName").value(prefix + " Khách tại A"))
        .andExpect(jsonPath("$.data.content[0].bookingCount").value(2))
        .andExpect(jsonPath("$.data.content[0].hotelCount").value(2))
        .andExpect(jsonPath("$.data.content[0].completedBookingCount").value(1))
        .andExpect(
            jsonPath("$.data.content[0].lastBookedAt").value(NOW.minusSeconds(100).toString()));
  }

  @Test
  void shouldPaginateDistinctBookersAndTreatSearchWildcardsAsLiteralText() throws Exception {
    booking(customerX, hotelA, "Khách%", "CONFIRMED", NOW);
    booking(customerX, hotelA2, "Khách cũ", "EXPIRED", NOW.minusSeconds(200));
    booking(customerY, hotelA, "Khách thường", "CANCELLED", NOW.minusSeconds(100));
    list("/management/customers", ownerA, "HOTEL_OWNER", "page=0&size=1")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$.data.content[0].id").value(customerX.toString()));
    list("/management/customers", ownerA, "HOTEL_OWNER", "page=1&size=1")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].id").value(customerY.toString()));
    mvc.perform(
            get("/api/v1/management/customers")
                .param("keyword", "%")
                .with(actor(ownerA, "HOTEL_OWNER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));
  }

  @Test
  void shouldKeepActivatedExpiredAndTerminatedPartnersButExcludeDraftsAndUnknownHistory()
      throws Exception {
    contract(ownerA, hotelA, "ACTIVE", NOW.minusSeconds(100), NOW.plusSeconds(100));
    contract(ownerA, hotelA2, "EXPIRED", NOW.minusSeconds(300), NOW.minusSeconds(200));
    UUID terminated =
        contract(ownerB, hotelB, "ACTIVE", NOW.minusSeconds(200), NOW.plusSeconds(200));
    jdbc.update("UPDATE hotel_contracts SET status='TERMINATED' WHERE id=?", terminated);
    UUID draftOwner = user("HOTEL_OWNER", "C Draft");
    contract(draftOwner, hotel(draftOwner), "DRAFT", NOW, NOW.plusSeconds(100));
    UUID unknownOwner = user("HOTEL_OWNER", "D Unknown");
    contract(
        unknownOwner,
        hotel(unknownOwner),
        "TERMINATED",
        NOW.minusSeconds(200),
        NOW.plusSeconds(100));
    UUID cancelledDraftOwner = user("HOTEL_OWNER", "E Cancelled draft");
    UUID cancelled =
        contract(
            cancelledDraftOwner, hotel(cancelledDraftOwner), "DRAFT", NOW, NOW.plusSeconds(100));
    jdbc.update("UPDATE hotel_contracts SET status='TERMINATED' WHERE id=?", cancelled);
    list("/admin/customers", admin, "ADMIN", "keyword=" + prefix)
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$.data.content[0].id").value(ownerA.toString()))
        .andExpect(jsonPath("$.data.content[0].contractCount").value(2))
        .andExpect(jsonPath("$.data.content[0].hotelCount").value(2))
        .andExpect(jsonPath("$.data.content[0].activeContractCount").value(1))
        .andExpect(jsonPath("$.data.content[1].id").value(ownerB.toString()))
        .andExpect(jsonPath("$.data.content[1].activeContractCount").value(0));
  }

  @Test
  void shouldSearchAndPaginatePartnerProfilesOnTheServer() throws Exception {
    contract(ownerA, hotelA, "ACTIVE", NOW.minusSeconds(10), NOW.plusSeconds(10));
    contract(ownerB, hotelB, "ACTIVE", NOW.minusSeconds(10), NOW.plusSeconds(10));
    list("/admin/customers", admin, "ADMIN", "keyword=" + prefix + "&page=1&size=1")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$.data.content[0].id").value(ownerB.toString()));
    mvc.perform(
            get("/api/v1/admin/customers")
                .param("keyword", ownerA + "@directory.test")
                .with(actor(admin, "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].id").value(ownerA.toString()));
  }

  @Test
  void shouldEnforceRoleAndAccountStatusInBothDirectories() throws Exception {
    list("/admin/customers", ownerA, "HOTEL_OWNER", "").andExpect(status().isForbidden());
    list("/management/customers", admin, "ADMIN", "").andExpect(status().isForbidden());
    list("/management/customers", customerX, "CUSTOMER", "").andExpect(status().isForbidden());
    list("/admin/customers", ownerA, "ADMIN", "").andExpect(status().isForbidden());
    jdbc.update("UPDATE users SET status='inactive' WHERE id IN (?,?)", admin, ownerA);
    list("/admin/customers", admin, "ADMIN", "").andExpect(status().isForbidden());
    list("/management/customers", ownerA, "HOTEL_OWNER", "").andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/admin/customers")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/management/customers")).andExpect(status().isUnauthorized());
  }

  @Test
  void shouldRejectInvalidQueriesAndReturnEmptyPagesWithoutUnrelatedContacts() throws Exception {
    list("/management/customers", ownerA, "HOTEL_OWNER", "page=-1")
        .andExpect(status().isBadRequest());
    list("/admin/customers", admin, "ADMIN", "size=1000000").andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/admin/customers")
                .param("keyword", "x".repeat(101))
                .with(actor(admin, "ADMIN")))
        .andExpect(status().isBadRequest());
    list("/management/customers", ownerA, "HOTEL_OWNER", "")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(0));
    list("/admin/customers", admin, "ADMIN", "keyword=" + prefix)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(0));
  }

  @Test
  void shouldPreserveActivationEvidenceWhenTerminatingAndRejectClearingTheFlag() {
    UUID active = contract(ownerA, hotelA, "ACTIVE", NOW.minusSeconds(10), NOW.plusSeconds(10));
    jdbc.update(
        "UPDATE hotel_contracts SET status='TERMINATED', has_been_activated=false WHERE id=?",
        active);
    assertThat(
            jdbc.queryForObject(
                "SELECT has_been_activated FROM hotel_contracts WHERE id=?", Boolean.class, active))
        .isTrue();
    UUID draft = contract(ownerB, hotelB, "DRAFT", NOW, NOW.plusSeconds(10));
    jdbc.update("UPDATE hotel_contracts SET status='TERMINATED' WHERE id=?", draft);
    assertThat(
            jdbc.queryForObject(
                "SELECT has_been_activated FROM hotel_contracts WHERE id=?", Boolean.class, draft))
        .isFalse();
  }

  @Test
  void shouldBackfillOnlyKnownActivationStatesWhenMigratingLegacyContracts() throws Exception {
    String script =
        new org.springframework.core.io.ClassPathResource(
                "db/migration/V20261124160000000__track_contract_activation.sql")
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    transaction.executeWithoutResult(
        tx -> {
          tx.setRollbackOnly();
          jdbc.execute("CREATE SCHEMA customer_contract_upgrade_test");
          jdbc.execute("SET LOCAL search_path TO customer_contract_upgrade_test, public");
          jdbc.execute("CREATE TABLE hotel_contracts(owner_user_id uuid, status text)");
          jdbc.execute(
              "INSERT INTO hotel_contracts(status) VALUES ('DRAFT'),('ACTIVE'),('EXPIRED'),('TERMINATED')");
          jdbc.execute(script);
          assertThat(
                  jdbc.queryForList(
                      "SELECT status FROM hotel_contracts WHERE has_been_activated", String.class))
              .containsExactlyInAnyOrder("ACTIVE", "EXPIRED");
          assertThat(
                  jdbc.queryForList(
                      "SELECT status FROM hotel_contracts WHERE NOT has_been_activated",
                      String.class))
              .containsExactlyInAnyOrder("DRAFT", "TERMINATED");
        });
  }

  private ResultActions list(String path, UUID actorId, String role, String query)
      throws Exception {
    return mvc.perform(
        get("/api/v1" + path + (query.isBlank() ? "" : "?" + query)).with(actor(actorId, role)));
  }

  private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
          .JwtRequestPostProcessor
      actor(UUID id, String role) {
    return jwt()
        .jwt(token -> token.subject(id.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  private UUID user(String role, String name) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO users(id,email,password_hash,full_name,phone,status,email_verified_at) VALUES (?,?,'unused',?,?,'active',now())",
        id,
        id + "@directory.test",
        prefix + " " + name,
        "+84" + Integer.toUnsignedString(id.hashCode()));
    jdbc.update(
        "INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE role_key=?",
        id,
        role);
    return id;
  }

  private UUID hotel(UUID owner) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO hotels(id,owner_user_id,city_id,name,slug,address,status) VALUES (?,?,'822cafba-73f6-4ec0-bae7-71804535cae3',? ,?,'Test','ACTIVE')",
        id,
        owner,
        prefix,
        "cd-" + id);
    return id;
  }

  private UUID contract(UUID owner, UUID hotel, String status, Instant start, Instant end) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO hotel_contracts(id,contract_code,hotel_id,owner_user_id,commission_rate,commission_basis,starts_at,ends_at,status,created_by_user_id)
        VALUES (?,?,?,?,10,'AFTER_DISCOUNT',?,?,?::hotel_contract_status__t,?)
        """,
        id,
        "CD-" + id,
        hotel,
        owner,
        java.sql.Timestamp.from(start),
        java.sql.Timestamp.from(end),
        status,
        admin);
    return id;
  }

  private void booking(UUID customer, UUID hotel, String name, String status, Instant created) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO bookings(id,booking_code,customer_id,hotel_id,booker_name_snapshot,booker_email_snapshot,booker_phone_snapshot,
            guest_name_snapshot,guest_email_snapshot,guest_phone_snapshot,check_in,check_out,days,nights,status,created_at)
        VALUES (?,?,?,?,?,'contact@directory.test','0900000000','Khách ở','guest@directory.test','0911111111',
            '2027-06-01','2027-06-02',2,1,?::booking_status__t,?)
        """,
        id,
        "CD-" + id.toString().substring(0, 12),
        customer,
        hotel,
        prefix + " " + name,
        status,
        java.sql.Timestamp.from(created));
  }
}
