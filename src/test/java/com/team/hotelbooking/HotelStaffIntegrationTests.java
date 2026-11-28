package com.team.hotelbooking;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Quản lý nhân sự khách sạn và quyền vận hành của nhân viên")
class HotelStaffIntegrationTests {

  private static final Instant NOW = Instant.parse("2027-02-01T08:00:00Z");
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;
  @MockitoBean Clock clock;

  UUID admin, owner, otherOwner, managerUser, receptionistUser, outsiderUser, unverifiedUser;
  String managerEmail, receptionistEmail, unverifiedEmail, ownerEmail, adminEmail;
  UUID hotel, otherHotel, roomType, room, bookingId, bookingItemId;

  @BeforeEach
  void setUp() {
    given(clock.instant()).willReturn(NOW);
    given(clock.getZone()).willReturn(ZoneOffset.UTC);
    given(clock.withZone(org.mockito.ArgumentMatchers.any()))
        .willAnswer(i -> Clock.fixed(clock.instant(), i.getArgument(0)));

    admin = createUser("ADMIN", "Admin StaffTest", true);
    adminEmail = emailOf(admin);
    owner = createUser("HOTEL_OWNER", "Owner StaffTest", true);
    ownerEmail = emailOf(owner);
    otherOwner = createUser("HOTEL_OWNER", "Other Owner", true);
    managerUser = createUser("CUSTOMER", "Manager User", true);
    managerEmail = emailOf(managerUser);
    receptionistUser = createUser("CUSTOMER", "Receptionist User", true);
    receptionistEmail = emailOf(receptionistUser);
    outsiderUser = createUser("CUSTOMER", "Outsider User", true);
    unverifiedUser = createUser("CUSTOMER", "Unverified User", false);
    unverifiedEmail = emailOf(unverifiedUser);

    hotel = createHotel(owner, "Staff Test Hotel");
    otherHotel = createHotel(otherOwner, "Other Hotel");
    roomType = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO room_types(id,hotel_id,name,max_occupancy,max_adults,max_children,base_price,is_active) VALUES (?,?,'Deluxe',2,2,0,1000000,TRUE)",
        roomType,
        hotel);
    room = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO rooms(id,hotel_id,room_type_id,room_number,readiness,operational_from) VALUES (?,?,?,'101','READY','2026-01-01T00:00:00Z')",
        room,
        hotel,
        roomType);

    bookingId = UUID.randomUUID();
    bookingItemId = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO bookings(id,booking_code,customer_id,hotel_id,
          booker_name_snapshot,booker_email_snapshot,booker_phone_snapshot,
          guest_name_snapshot,guest_email_snapshot,guest_phone_snapshot,
          check_in,check_out,days,nights,status,room_subtotal_amount,subtotal_amount,total_amount,created_at)
        VALUES (?,?,?,?,'Khách đặt','guest@test.local','0900000000',
          'Khách ở','guest@test.local','0900000000',
          '2027-02-01','2027-02-03',3,2,'CONFIRMED',2000000,2000000,2000000,?)
        """,
        bookingId,
        "ST-" + bookingId.toString().substring(0, 10),
        outsiderUser,
        hotel,
        java.sql.Timestamp.from(NOW.minusSeconds(3600)));
    jdbc.update(
        """
        INSERT INTO booking_items(id,booking_id,hotel_id,room_type_id,room_type_name_snapshot,quantity,adults,children,extra_guests,room_subtotal_amount,subtotal_amount)
        VALUES (?,?,?,?,'Deluxe',1,2,0,0,2000000,2000000)
        """,
        bookingItemId,
        bookingId,
        hotel,
        roomType);
    for (String night : new String[] {"2027-02-01", "2027-02-02"}) {
      jdbc.update(
          """
          INSERT INTO inventory_reservations(id,booking_id,booking_item_id,room_type_id,stay_date,quantity,status,confirmed_at)
          VALUES (?,?,?,?,CAST(? AS date),1,'CONFIRMED',?)
          """,
          UUID.randomUUID(),
          bookingId,
          bookingItemId,
          roomType,
          night,
          java.sql.Timestamp.from(NOW.minusSeconds(3500)));
    }
  }

  @Test
  void ownerCanAddRevokeReactivateAndDetectVersionConflict() throws Exception {
    // Owner sees their hotel in staff-hotels with canAssignManager=true
    mvc.perform(get("/api/v1/management/staff-hotels").with(actor(owner, "HOTEL_OWNER")))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.data.content[0].id").value(hotel.toString()))
        .andExpect(jsonPath("$.data.content[0].canAssignManager").value(true));

    // Add receptionist by email (uppercase + whitespace normalized)
    String created =
        mvc.perform(
                post("/api/v1/management/hotels/" + hotel + "/members")
                    .with(actor(owner, "HOTEL_OWNER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\" "
                            + receptionistEmail.toUpperCase()
                            + " \",\"memberRole\":\"RECEPTIONIST\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.data.userId").value(receptionistUser.toString()))
            .andExpect(jsonPath("$.data.email").value(receptionistEmail))
            .andExpect(jsonPath("$.data.memberRole").value("RECEPTIONIST"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.version").value(0))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID memberId = UUID.fromString(data(created).path("id").asText());

    // Duplicate active assignment (same or different role) -> 409
    mvc.perform(
            post("/api/v1/management/hotels/" + hotel + "/members")
                .with(actor(owner, "HOTEL_OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"" + receptionistEmail + "\",\"memberRole\":\"HOTEL_MANAGER\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("HOTEL_MEMBER_ALREADY_EXISTS"));

    // Revoke -> version becomes 1
    mvc.perform(
            patch("/api/v1/management/hotels/" + hotel + "/members/" + memberId)
                .with(actor(owner, "HOTEL_OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REVOKED\",\"expectedVersion\":0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("REVOKED"))
        .andExpect(jsonPath("$.data.version").value(1));

    // Stale version -> 409
    mvc.perform(
            patch("/api/v1/management/hotels/" + hotel + "/members/" + memberId)
                .with(actor(owner, "HOTEL_OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACTIVE\",\"expectedVersion\":0}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("HOTEL_MEMBER_VERSION_CONFLICT"));

    // Reactivate via POST same role reuses existing row and increments version
    mvc.perform(
            post("/api/v1/management/hotels/" + hotel + "/members")
                .with(actor(owner, "HOTEL_OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"" + receptionistEmail + "\",\"memberRole\":\"RECEPTIONIST\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.id").value(memberId.toString()))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
        .andExpect(jsonPath("$.data.version").value(2));
  }

  @Test
  void roleBoundariesAndTargetAccountEligibilityAreEnforced() throws Exception {
    // Owner assigns manager
    String mgrJson =
        mvc.perform(
                post("/api/v1/management/hotels/" + hotel + "/members")
                    .with(actor(owner, "HOTEL_OWNER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"" + managerEmail + "\",\"memberRole\":\"HOTEL_MANAGER\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID mgrMemberId = UUID.fromString(data(mgrJson).path("id").asText());

    // Manager sees hotel in staff-hotels with canAssignManager=false
    mvc.perform(get("/api/v1/management/staff-hotels").with(actor(managerUser, "CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].id").value(hotel.toString()))
        .andExpect(jsonPath("$.data.content[0].canAssignManager").value(false));

    // Manager CAN add receptionist
    mvc.perform(
            post("/api/v1/management/hotels/" + hotel + "/members")
                .with(actor(managerUser, "CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"" + receptionistEmail + "\",\"memberRole\":\"RECEPTIONIST\"}"))
        .andExpect(status().isCreated());

    // Manager CANNOT add another HOTEL_MANAGER -> 403
    mvc.perform(
            post("/api/v1/management/hotels/" + hotel + "/members")
                .with(actor(managerUser, "CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\""
                        + emailOf(outsiderUser)
                        + "\",\"memberRole\":\"HOTEL_MANAGER\"}"))
        .andExpect(status().isForbidden());

    // Manager CANNOT revoke self -> 403
    mvc.perform(
            patch("/api/v1/management/hotels/" + hotel + "/members/" + mgrMemberId)
                .with(actor(managerUser, "CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REVOKED\",\"expectedVersion\":0}"))
        .andExpect(status().isForbidden());

    // Receptionist -> 403 on staff list/add
    mvc.perform(
            get("/api/v1/management/hotels/" + hotel + "/members")
                .with(actor(receptionistUser, "CUSTOMER")))
        .andExpect(status().isForbidden());

    // Outsider -> 404 on hotel members
    mvc.perform(
            get("/api/v1/management/hotels/" + hotel + "/members")
                .with(actor(outsiderUser, "CUSTOMER")))
        .andExpect(status().isNotFound());

    // Admin -> 403 on staff management
    mvc.perform(get("/api/v1/management/staff-hotels").with(actor(admin, "ADMIN")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/management/hotels/" + hotel + "/members").with(actor(admin, "ADMIN")))
        .andExpect(status().isForbidden());

    // Unverified / owner / admin / unknown target -> 422 with email field error
    for (String badEmail :
        new String[] {unverifiedEmail, ownerEmail, adminEmail, "missing@nowhere.test"}) {
      mvc.perform(
              post("/api/v1/management/hotels/" + hotel + "/members")
                  .with(actor(owner, "HOTEL_OWNER"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"email\":\"" + badEmail + "\",\"memberRole\":\"RECEPTIONIST\"}"))
          .andExpect(status().isUnprocessableContent())
          .andExpect(jsonPath("$.code").value("STAFF_ACCOUNT_NOT_ELIGIBLE"))
          .andExpect(jsonPath("$.errors[0].field").value("email"));
    }
  }

  @Test
  void assignedReceptionistCanOperateBookingsAndLosesAccessWhenRevoked() throws Exception {
    String created =
        mvc.perform(
                post("/api/v1/management/hotels/" + hotel + "/members")
                    .with(actor(owner, "HOTEL_OWNER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\""
                            + receptionistEmail
                            + "\",\"memberRole\":\"RECEPTIONIST\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID memberId = UUID.fromString(data(created).path("id").asText());

    // /me/workplaces returns active assignment
    mvc.perform(get("/api/v1/me/workplaces").with(actor(receptionistUser, "CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].hotelId").value(hotel.toString()))
        .andExpect(jsonPath("$.data[0].memberRole").value("RECEPTIONIST"));

    // GET /management/hotels returns ONLY assigned hotel with accessLevel=RECEPTIONIST
    mvc.perform(get("/api/v1/management/hotels").with(actor(receptionistUser, "CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].id").value(hotel.toString()))
        .andExpect(jsonPath("$.data.content[0].accessLevel").value("RECEPTIONIST"));

    // Other hotel -> 404
    mvc.perform(
            get("/api/v1/management/hotels/" + otherHotel)
                .with(actor(receptionistUser, "CUSTOMER")))
        .andExpect(status().isNotFound());

    // Bookings list, detail, rooms list, assign room, check-in all succeed for receptionist
    mvc.perform(
            get("/api/v1/management/hotels/" + hotel + "/bookings")
                .with(actor(receptionistUser, "CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));

    mvc.perform(
            get("/api/v1/management/hotels/" + hotel + "/bookings/" + bookingId)
                .with(actor(receptionistUser, "CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(bookingId.toString()));

    mvc.perform(
            get("/api/v1/management/hotels/" + hotel + "/rooms")
                .with(actor(receptionistUser, "CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));

    mvc.perform(
            put("/api/v1/management/hotels/"
                    + hotel
                    + "/bookings/"
                    + bookingId
                    + "/room-assignments")
                .with(actor(receptionistUser, "CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"assignments\":[{\"bookingItemId\":\""
                        + bookingItemId
                        + "\",\"roomIds\":[\""
                        + room
                        + "\"]}]}"))
        .andExpect(status().isOk());

    mvc.perform(
            post("/api/v1/management/hotels/" + hotel + "/bookings/" + bookingId + "/check-in")
                .with(actor(receptionistUser, "CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.stayStatus").value("CHECKED_IN"));

    // Revoke receptionist -> loses all operational access
    mvc.perform(
            patch("/api/v1/management/hotels/" + hotel + "/members/" + memberId)
                .with(actor(owner, "HOTEL_OWNER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REVOKED\",\"expectedVersion\":0}"))
        .andExpect(status().isOk());

    mvc.perform(get("/api/v1/me/workplaces").with(actor(receptionistUser, "CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(0));

    mvc.perform(get("/api/v1/management/hotels").with(actor(receptionistUser, "CUSTOMER")))
        .andExpect(status().isForbidden());

    mvc.perform(
            get("/api/v1/management/hotels/" + hotel + "/bookings")
                .with(actor(receptionistUser, "CUSTOMER")))
        .andExpect(status().isForbidden());
  }

  private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
          .JwtRequestPostProcessor
      actor(UUID id, String role) {
    return jwt()
        .jwt(token -> token.subject(id.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  private UUID createUser(String role, String name, boolean verified) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO users(id,email,password_hash,full_name,phone,status,email_verified_at) VALUES (?,?,'unused',?,?,'active',?)",
        id,
        emailOf(id),
        name,
        "+84" + Integer.toUnsignedString(id.hashCode()),
        verified ? java.sql.Timestamp.from(NOW.minusSeconds(86400)) : null);
    jdbc.update(
        "INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE role_key=?",
        id,
        role);
    return id;
  }

  private String emailOf(UUID id) {
    return id + "@staff.test";
  }

  private UUID createHotel(UUID ownerId, String name) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO hotels(id,owner_user_id,city_id,name,slug,address,status) VALUES (?,?,'822cafba-73f6-4ec0-bae7-71804535cae3',?,?,'Da Nang','ACTIVE')",
        id,
        ownerId,
        name,
        "staff-" + id);
    return id;
  }

  private JsonNode data(String json) throws Exception {
    return MAPPER.readTree(json).path("data");
  }
}
