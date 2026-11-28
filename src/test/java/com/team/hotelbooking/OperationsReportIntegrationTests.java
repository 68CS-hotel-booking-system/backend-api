package com.team.hotelbooking;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Báo cáo vận hành (occupancy, ADR, RevPAR, doanh thu theo tháng)")
class OperationsReportIntegrationTests {

  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;

  UUID admin, ownerA, ownerB, customer;
  UUID hotelA, hotelB, roomTypeA;

  @BeforeEach
  void setUp() {
    admin = createUser("ADMIN", "Admin Report");
    ownerA = createUser("HOTEL_OWNER", "Owner A Report");
    ownerB = createUser("HOTEL_OWNER", "Owner B Report");
    customer = createUser("CUSTOMER", "Customer Report");

    hotelA = createHotel(ownerA, "Report Hotel A");
    hotelB = createHotel(ownerB, "Report Hotel B");

    roomTypeA = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO room_types(id,hotel_id,name,max_occupancy,max_adults,max_children,base_price,is_active) VALUES (?,?,'Standard',2,2,0,500000,TRUE)",
        roomTypeA,
        hotelA);
    // 2 operational rooms in hotelA
    for (int i = 1; i <= 2; i++) {
      jdbc.update(
          "INSERT INTO rooms(id,hotel_id,room_type_id,room_number,readiness,operational_from) VALUES (?,?,?,?,'READY','2026-01-01T00:00:00Z')",
          UUID.randomUUID(),
          hotelA,
          roomTypeA,
          "R10" + i);
    }

    // 1) CONFIRMED booking: 2 nights (2027-03-10, 2027-03-11), qty=1, unit_price=500000
    UUID confirmed =
        insertBooking(
            hotelA,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            "CONFIRMED",
            1_000_000,
            1_000_000,
            Instant.parse("2027-03-05T03:00:00Z"));
    UUID item1 = insertItem(confirmed, hotelA, roomTypeA, 1, 1_000_000);
    insertNightPrice(item1, LocalDate.of(2027, 3, 10), 500_000);
    insertNightPrice(item1, LocalDate.of(2027, 3, 11), 500_000);

    // 2) CHECKED_OUT booking: 1 night (2027-03-15), qty=1, unit_price=800000, total=750000 (after
    // discount)
    UUID checkedOut =
        insertBooking(
            hotelA,
            LocalDate.of(2027, 3, 15),
            LocalDate.of(2027, 3, 16),
            "CHECKED_OUT",
            800_000,
            750_000,
            Instant.parse("2027-03-10T04:00:00Z"));
    UUID item2 = insertItem(checkedOut, hotelA, roomTypeA, 1, 800_000);
    insertNightPrice(item2, LocalDate.of(2027, 3, 15), 800_000);

    // 3) CANCELLED booking: excluded from sold/roomRevenue, counted in cancelledBookings
    UUID cancelled =
        insertBooking(
            hotelA,
            LocalDate.of(2027, 3, 18),
            LocalDate.of(2027, 3, 19),
            "CANCELLED",
            500_000,
            500_000,
            Instant.parse("2027-03-12T04:00:00Z"));
    UUID item3 = insertItem(cancelled, hotelA, roomTypeA, 1, 500_000);
    insertNightPrice(item3, LocalDate.of(2027, 3, 18), 500_000);
    jdbc.update(
        "INSERT INTO booking_status_history(id,booking_id,from_status,to_status,created_at) VALUES (?,?,'CONFIRMED','CANCELLED',?)",
        UUID.randomUUID(),
        cancelled,
        java.sql.Timestamp.from(Instant.parse("2027-03-13T05:00:00Z")));

    // 4) NO_SHOW booking on 2027-03-20
    insertBooking(
        hotelA,
        LocalDate.of(2027, 3, 20),
        LocalDate.of(2027, 3, 21),
        "NO_SHOW",
        500_000,
        500_000,
        Instant.parse("2027-03-14T05:00:00Z"));
  }

  @Test
  void shouldCalculateOccupancyAdrRevparAndMonthlyBreakdown() throws Exception {
    // 2 rooms * 31 days in March 2027 = 62 available room nights
    // Sold = 2 (CONFIRMED) + 1 (CHECKED_OUT) = 3
    // Room revenue = 1,000,000 + 800,000 = 1,800,000
    // Occupancy = round(3 / 62, 4) = 0.0484
    // ADR = 1,800,000 / 3 = 600,000; RevPAR = 1,800,000 / 62 = 29,032
    mvc.perform(
            get("/api/v1/management/reports/operations")
                .param("from", "2027-03-01")
                .param("to", "2027-03-31")
                .with(actor(ownerA, "HOTEL_OWNER")))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.data.currency").value("VND"))
        .andExpect(jsonPath("$.data.totals.availableRoomNights").value(62))
        .andExpect(jsonPath("$.data.totals.soldRoomNights").value(3))
        .andExpect(jsonPath("$.data.totals.roomRevenue").value(1_800_000))
        .andExpect(jsonPath("$.data.totals.occupancyRate").value(0.0484))
        .andExpect(jsonPath("$.data.totals.adr").value(600_000))
        .andExpect(jsonPath("$.data.totals.revpar").value(29_032))
        .andExpect(jsonPath("$.data.totals.completedRevenue").value(750_000))
        .andExpect(jsonPath("$.data.totals.newBookings").value(4))
        .andExpect(jsonPath("$.data.totals.cancelledBookings").value(1))
        .andExpect(jsonPath("$.data.totals.noShowBookings").value(1))
        .andExpect(jsonPath("$.data.months.length()").value(1))
        .andExpect(jsonPath("$.data.months[0].month").value("2027-03"))
        .andExpect(jsonPath("$.data.hotels.length()").value(1))
        .andExpect(jsonPath("$.data.hotels[0].hotelId").value(hotelA.toString()))
        .andExpect(jsonPath("$.data.scopeHotels.length()").value(1));

    // Multi-month query includes empty February 2027 row with zeros
    mvc.perform(
            get("/api/v1/management/reports/operations")
                .param("from", "2027-02-01")
                .param("to", "2027-03-31")
                .with(actor(ownerA, "HOTEL_OWNER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.months.length()").value(2))
        .andExpect(jsonPath("$.data.months[0].month").value("2027-02"))
        .andExpect(jsonPath("$.data.months[0].soldRoomNights").value(0))
        .andExpect(jsonPath("$.data.months[0].availableRoomNights").value(56))
        .andExpect(jsonPath("$.data.months[1].month").value("2027-03"));
  }

  @Test
  void shouldEnforceScopeDateValidationAndRoleAccess() throws Exception {
    // Owner A asking for Owner B's hotel -> 404
    mvc.perform(
            get("/api/v1/management/reports/operations")
                .param("from", "2027-03-01")
                .param("to", "2027-03-31")
                .param("hotelId", hotelB.toString())
                .with(actor(ownerA, "HOTEL_OWNER")))
        .andExpect(status().isNotFound());

    // Admin can filter by hotelA
    mvc.perform(
            get("/api/v1/management/reports/operations")
                .param("from", "2027-03-01")
                .param("to", "2027-03-31")
                .param("hotelId", hotelA.toString())
                .with(actor(admin, "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.hotels.length()").value(1))
        .andExpect(jsonPath("$.data.totals.soldRoomNights").value(3));

    // Invalid date range: from > to -> 400
    mvc.perform(
            get("/api/v1/management/reports/operations")
                .param("from", "2027-04-01")
                .param("to", "2027-03-01")
                .with(actor(ownerA, "HOTEL_OWNER")))
        .andExpect(status().isBadRequest());

    // Range > 366 days -> 400
    mvc.perform(
            get("/api/v1/management/reports/operations")
                .param("from", "2026-01-01")
                .param("to", "2027-03-01")
                .with(actor(ownerA, "HOTEL_OWNER")))
        .andExpect(status().isBadRequest());

    // Missing from -> 400
    mvc.perform(
            get("/api/v1/management/reports/operations")
                .param("to", "2027-03-31")
                .with(actor(ownerA, "HOTEL_OWNER")))
        .andExpect(status().isBadRequest());

    // Customer -> 403
    mvc.perform(
            get("/api/v1/management/reports/operations")
                .param("from", "2027-03-01")
                .param("to", "2027-03-31")
                .with(actor(customer, "CUSTOMER")))
        .andExpect(status().isForbidden());
  }

  private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
          .JwtRequestPostProcessor
      actor(UUID id, String role) {
    return jwt()
        .jwt(token -> token.subject(id.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  private UUID createUser(String role, String name) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO users(id,email,password_hash,full_name,phone,status,email_verified_at) VALUES (?,?,'unused',?,?,'active',now())",
        id,
        id + "@report.test",
        name,
        "+84" + Integer.toUnsignedString(id.hashCode()));
    jdbc.update(
        "INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE role_key=?",
        id,
        role);
    return id;
  }

  private UUID createHotel(UUID ownerId, String name) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO hotels(id,owner_user_id,city_id,name,slug,address,status) VALUES (?,?,'822cafba-73f6-4ec0-bae7-71804535cae3',?,?,'Hanoi','ACTIVE')",
        id,
        ownerId,
        name,
        "rpt-" + id);
    return id;
  }

  private UUID insertBooking(
      UUID hotelId,
      LocalDate checkIn,
      LocalDate checkOut,
      String status,
      long roomSubtotal,
      long totalAmount,
      Instant createdAt) {
    UUID id = UUID.randomUUID();
    int nights = (int) (checkOut.toEpochDay() - checkIn.toEpochDay());
    long discount = Math.max(0L, roomSubtotal - totalAmount);
    jdbc.update(
        """
        INSERT INTO bookings(id,booking_code,customer_id,hotel_id,
          booker_name_snapshot,booker_email_snapshot,booker_phone_snapshot,
          guest_name_snapshot,guest_email_snapshot,guest_phone_snapshot,
          check_in,check_out,days,nights,status,
          room_subtotal_amount,subtotal_amount,discount_amount,total_amount,created_at)
        VALUES (?,?,?,?,'Khách','k@report.test','0900000000',
          'Khách','k@report.test','0900000000',
          ?,?,?,?,?::booking_status__t,?,?,?,?,?)
        """,
        id,
        "RP-" + id.toString().substring(0, 10),
        customer,
        hotelId,
        checkIn,
        checkOut,
        nights + 1,
        nights,
        status,
        roomSubtotal,
        roomSubtotal,
        discount,
        totalAmount,
        java.sql.Timestamp.from(createdAt));
    return id;
  }

  private UUID insertItem(
      UUID bookingId, UUID hotelId, UUID roomTypeId, int qty, long roomSubtotal) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO booking_items(id,booking_id,hotel_id,room_type_id,room_type_name_snapshot,quantity,adults,children,extra_guests,room_subtotal_amount,subtotal_amount)
        VALUES (?,?,?,?,'Standard',?,?,0,0,?,?)
        """,
        id,
        bookingId,
        hotelId,
        roomTypeId,
        qty,
        qty,
        roomSubtotal,
        roomSubtotal);
    return id;
  }

  private void insertNightPrice(UUID itemId, LocalDate stayDate, long unitPrice) {
    jdbc.update(
        "INSERT INTO booking_night_prices(booking_item_id,stay_date,unit_price) VALUES (?,?,?)",
        itemId,
        stayDate,
        unitPrice);
  }
}
