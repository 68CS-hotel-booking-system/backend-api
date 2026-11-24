package com.team.hotelbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Tách tài khoản quản trị khỏi đặt phòng cá nhân")
class BookingRoleAccessIntegrationTests {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @Test
  void shouldRejectAdminUsingAuthoritativeAccountRolesEvenIfJwtOmitsAdmin() throws Exception {
    UUID admin = user("ADMIN");
    for (String jwtRole : java.util.List.of("ADMIN", "CUSTOMER")) {
      create(admin, jwtRole)
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ADMIN_BOOKING_FORBIDDEN"));
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM bookings WHERE customer_id = ?", Long.class, admin))
        .isZero();
  }

  @Test
  void shouldRejectAdminEvenWhenAccountAlsoHasCustomerRole() throws Exception {
    UUID admin = user("ADMIN");
    jdbc.update(
        "INSERT INTO user_roles(user_id,role_id) SELECT ?, id FROM roles WHERE role_key = 'CUSTOMER'",
        admin);
    create(admin, "CUSTOMER")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ADMIN_BOOKING_FORBIDDEN"));
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM bookings WHERE customer_id = ?", Long.class, admin))
        .isZero();
  }

  @Test
  void shouldKeepNonAdminAccountsOnTheNormalHotelValidationPath() throws Exception {
    for (String role : java.util.List.of("CUSTOMER", "HOTEL_OWNER")) {
      create(user(role), role)
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("HOTEL_NOT_FOUND"));
    }
  }

  private ResultActions create(UUID actor, String jwtRole) throws Exception {
    return mvc.perform(
        post("/api/v1/bookings")
            .with(
                jwt()
                    .jwt(token -> token.subject(actor.toString()))
                    .authorities(new SimpleGrantedAuthority("ROLE_" + jwtRole)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
          {"hotelId":"%s","checkIn":"2099-01-10","checkOut":"2099-01-11",
           "booker":{"fullName":"Khách kiểm thử","email":"guest@example.test","phone":"0900000000"},
           "items":[{"roomTypeId":"%s","quantity":1,"adults":1,"children":0}]}
          """
                    .formatted(UUID.randomUUID(), UUID.randomUUID())));
  }

  private UUID user(String role) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO users(id,email,password_hash,full_name,phone,status,email_verified_at) VALUES (?,?,'unused','Test',?,'active',now())",
        id,
        id + "@booking-role.test",
        "+84" + Integer.toUnsignedString(id.hashCode()));
    jdbc.update(
        "INSERT INTO user_roles(user_id,role_id) SELECT ?, id FROM roles WHERE role_key = ?",
        id,
        role);
    return id;
  }
}
