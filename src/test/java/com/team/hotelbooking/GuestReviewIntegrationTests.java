package com.team.hotelbooking;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.team.hotelbooking.booking.review.application.command.SubmitGuestReviewCommand;
import com.team.hotelbooking.booking.review.application.usecase.SubmitGuestReviewUseCase;
import com.team.hotelbooking.booking.review.domain.exception.GuestReviewException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Đánh giá của khách đã hoàn thành lưu trú")
class GuestReviewIntegrationTests {
  private static final Instant NOW = Instant.parse("2027-01-20T00:00:00Z");
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired SubmitGuestReviewUseCase submit;
  @MockitoBean Clock clock;
  UUID customer, owner, admin, hotel;

  @BeforeEach
  void setup() {
    given(clock.instant()).willReturn(NOW);
    given(clock.getZone()).willReturn(ZoneOffset.UTC);
    given(clock.withZone(org.mockito.ArgumentMatchers.any()))
        .willAnswer(i -> Clock.fixed(clock.instant(), i.getArgument(0)));
    customer = user("CUSTOMER");
    owner = user("HOTEL_OWNER");
    admin = user("ADMIN");
    hotel = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO hotels(id,owner_user_id,city_id,name,slug,address,status) VALUES (?,?,'822cafba-73f6-4ec0-bae7-71804535cae3','Khách sạn đánh giá',?,'Đà Nẵng','ACTIVE')",
        hotel,
        owner,
        "guest-review-" + hotel);
  }

  @Test
  void completedCustomerCanReviewOnceAndPublicResponseProtectsPrivateData() throws Exception {
    UUID booking = booking("CHECKED_OUT");
    mvc.perform(get("/api/v1/bookings/" + booking + "/review").with(actor(customer, "CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.canReview").value(true));
    postReview(booking, customer, 5, " Phòng sạch, nhân viên thân thiện. ")
        .andExpect(status().isCreated())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.data.content").value("Phòng sạch, nhân viên thân thiện."));
    postReview(booking, customer, 4, "Đánh giá lần nữa.")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("GUEST_REVIEW_ALREADY_EXISTS"));
    mvc.perform(get("/api/v1/bookings/" + booking + "/review").with(actor(customer, "CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.canReview").value(false))
        .andExpect(jsonPath("$.data.review.rating").value(5));
    jdbc.update("UPDATE users SET full_name='Tên đã đổi' WHERE id=?", customer);
    publicList()
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.summary.totalReviews").value(1))
        .andExpect(jsonPath("$.data.summary.averageRating").value(5))
        .andExpect(jsonPath("$.data.reviews.content[0].authorDisplayName").value("N. Anh"))
        .andExpect(jsonPath("$.data.reviews.content[0].bookingId").doesNotExist())
        .andExpect(jsonPath("$.data.reviews.content[0].bookingCode").doesNotExist())
        .andExpect(jsonPath("$.data.reviews.content[0].customerId").doesNotExist())
        .andExpect(jsonPath("$.data.reviews.content[0].email").doesNotExist())
        .andExpect(jsonPath("$.data.reviews.content[0].canReply").doesNotExist());
  }

  @Test
  void otherBookingStatesCannotReviewAndUnauthenticatedCannotSubmit() throws Exception {
    for (String state :
        List.of("PENDING_PAYMENT", "CONFIRMED", "CHECKED_IN", "CANCELLED", "EXPIRED", "NO_SHOW")) {
      UUID id = booking(state);
      postReview(id, customer, 5, "Nhận xét.")
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("BOOKING_NOT_REVIEWABLE"));
      mvc.perform(get("/api/v1/bookings/" + id + "/review").with(actor(customer, "CUSTOMER")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.canReview").value(false));
    }
    UUID id = booking("CHECKED_OUT");
    mvc.perform(
            post("/api/v1/bookings/" + id + "/review")
                .contentType("application/json")
                .content("{\"rating\":5,\"content\":\"Nhận xét\"}"))
        .andExpect(status().isUnauthorized());
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM guest_reviews WHERE hotel_id=?", Long.class, hotel))
        .isZero();
  }

  @Test
  void ownershipAndActualAccountStateAreChecked() throws Exception {
    UUID id = booking("CHECKED_OUT");
    postReview(id, owner, 5, "Nhận xét.").andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/bookings/" + id + "/review").with(actor(owner, "CUSTOMER")))
        .andExpect(status().isNotFound());
    jdbc.update("UPDATE users SET email_verified_at=null WHERE id=?", customer);
    postReview(id, customer, 5, "Nhận xét.").andExpect(status().isForbidden());
    jdbc.update("UPDATE users SET email_verified_at=now(),status='inactive' WHERE id=?", customer);
    postReview(id, customer, 5, "Nhận xét.").andExpect(status().isForbidden());
  }

  @Test
  void validatesIntegerStarsTextAndUnknownFields() throws Exception {
    UUID id = booking("CHECKED_OUT");
    for (String body :
        List.of(
            "{\"rating\":0,\"content\":\"a\"}",
            "{\"rating\":6,\"content\":\"a\"}",
            "{\"rating\":4.5,\"content\":\"a\"}",
            "{\"content\":\"a\"}",
            "{\"rating\":5,\"content\":\"   \"}",
            "{\"rating\":5,\"content\":\"a\",\"hotelId\":\"" + hotel + "\"}"))
      mvc.perform(
              post("/api/v1/bookings/" + id + "/review")
                  .with(actor(customer, "CUSTOMER"))
                  .contentType("application/json")
                  .content(body))
          .andExpect(status().isBadRequest());
    postReview(id, customer, 5, "a".repeat(2001)).andExpect(status().isBadRequest());
    assertThatThrownBy(
            () -> submit.execute(new SubmitGuestReviewCommand(customer, id, 7, "Nhận xét")))
        .isInstanceOf(GuestReviewException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM guest_reviews WHERE booking_id=?", Long.class, id))
        .isZero();
  }

  @Test
  void publicSummaryUsesAllReviewsWhilePageFiltersAndRoundsMeanOnce() throws Exception {
    for (int rating : List.of(5, 5, 4))
      postReview(booking("CHECKED_OUT"), customer, rating, "Nhận xét " + rating)
          .andExpect(status().isCreated());
    mvc.perform(
            get("/api/v1/hotels/" + hotel + "/guest-reviews")
                .param("rating", "4")
                .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reviews.totalElements").value(1))
        .andExpect(jsonPath("$.data.reviews.content[0].rating").value(4))
        .andExpect(jsonPath("$.data.summary.totalReviews").value(3))
        .andExpect(jsonPath("$.data.summary.averageRating").value(4.7))
        .andExpect(jsonPath("$.data.summary.ratingCounts[0].rating").value(5))
        .andExpect(jsonPath("$.data.summary.ratingCounts[0].count").value(2));
    mvc.perform(
            get("/api/v1/hotels/" + hotel + "/guest-reviews").param("page", "1").param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reviews.content.length()").value(1))
        .andExpect(jsonPath("$.data.reviews.totalPages").value(3));
  }

  @Test
  void emptySummaryAndPaginationValidation() throws Exception {
    publicList()
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.summary.totalReviews").value(0))
        .andExpect(jsonPath("$.data.summary.averageRating").isEmpty());
    for (String query : List.of("?page=-1", "?size=0", "?size=1001", "?rating=6"))
      mvc.perform(get("/api/v1/hotels/" + hotel + "/guest-reviews" + query))
          .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/hotels/" + UUID.randomUUID() + "/guest-reviews"))
        .andExpect(status().isNotFound());
  }

  @Test
  void ownerCanReplyAndEditWithVersionWithoutChangingGuestContent() throws Exception {
    UUID booking = booking("CHECKED_OUT");
    postReview(booking, customer, 4, "Phòng tốt.").andExpect(status().isCreated());
    UUID review = review(booking);
    managed(owner, "HOTEL_OWNER")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reviews.content[0].canReply").value(true));
    managed(admin, "ADMIN")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reviews.content[0].canReply").value(false));
    reply(review, owner, "HOTEL_OWNER", 0, " Cảm ơn bạn đã góp ý. ")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.version").value(1));
    reply(review, owner, "HOTEL_OWNER", 0, "Ghi đè bản cũ")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("GUEST_REVIEW_VERSION_CONFLICT"));
    reply(review, owner, "HOTEL_OWNER", 1, "Khách sạn đã bổ sung dịch vụ theo góp ý.")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.rating").value(4))
        .andExpect(jsonPath("$.data.content").value("Phòng tốt."));
    publicList()
        .andExpect(
            jsonPath("$.data.reviews.content[0].reply")
                .value("Khách sạn đã bổ sung dịch vụ theo góp ý."));
    mvc.perform(get("/api/v1/bookings/" + booking + "/review").with(actor(customer, "CUSTOMER")))
        .andExpect(
            jsonPath("$.data.review.reply").value("Khách sạn đã bổ sung dịch vụ theo góp ý."));
  }

  @Test
  void ownerScopeAndActualRolesCannotBeForgedAndAdminIsReadOnly() throws Exception {
    UUID booking = booking("CHECKED_OUT");
    postReview(booking, customer, 5, "Tốt.").andExpect(status().isCreated());
    UUID review = review(booking);
    UUID stranger = user("HOTEL_OWNER");
    managed(stranger, "HOTEL_OWNER").andExpect(status().isForbidden());
    reply(review, stranger, "HOTEL_OWNER", 0, "Phản hồi").andExpect(status().isForbidden());
    reply(review, customer, "HOTEL_OWNER", 0, "Giả owner").andExpect(status().isForbidden());
    reply(review, admin, "HOTEL_OWNER", 0, "Admin giả owner").andExpect(status().isForbidden());
    reply(review, admin, "ADMIN", 0, "Admin phản hồi").andExpect(status().isForbidden());
    managed(customer, "HOTEL_OWNER").andExpect(status().isForbidden());
    reply(UUID.randomUUID(), owner, "HOTEL_OWNER", 0, "Phản hồi").andExpect(status().isNotFound());
  }

  @Test
  void hotelTransferMovesReplyPermissionToNewOwner() throws Exception {
    UUID booking = booking("CHECKED_OUT");
    postReview(booking, customer, 5, "Tốt.").andExpect(status().isCreated());
    UUID review = review(booking);
    UUID next = user("HOTEL_OWNER");
    jdbc.update("UPDATE hotels SET owner_user_id=? WHERE id=?", next, hotel);
    reply(review, owner, "HOTEL_OWNER", 0, "Chủ cũ").andExpect(status().isForbidden());
    reply(review, next, "HOTEL_OWNER", 0, "Chủ mới cảm ơn bạn.").andExpect(status().isOk());
    assertThat(
            jdbc.queryForObject(
                "SELECT replied_by FROM guest_reviews WHERE id=?", UUID.class, review))
        .isEqualTo(next);
  }

  @Test
  void inactiveHotelReviewsStayPrivateButOwnerCanStillRespond() throws Exception {
    UUID booking = booking("CHECKED_OUT");
    postReview(booking, customer, 5, "Tốt.").andExpect(status().isCreated());
    jdbc.update("UPDATE hotels SET status='SUSPENDED' WHERE id=?", hotel);
    publicList().andExpect(status().isNotFound());
    managed(owner, "HOTEL_OWNER").andExpect(status().isOk());
    reply(review(booking), owner, "HOTEL_OWNER", 0, "Cảm ơn bạn.").andExpect(status().isOk());
  }

  @Test
  void managementReplyFiltersAreServerSide() throws Exception {
    UUID first = booking("CHECKED_OUT"), second = booking("CHECKED_OUT");
    postReview(first, customer, 5, "Tốt.").andExpect(status().isCreated());
    postReview(second, customer, 3, "Ổn.").andExpect(status().isCreated());
    reply(review(first), owner, "HOTEL_OWNER", 0, "Cảm ơn").andExpect(status().isOk());
    mvc.perform(
            get("/api/v1/management/hotels/" + hotel + "/guest-reviews")
                .param("replied", "false")
                .with(actor(owner, "HOTEL_OWNER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reviews.totalElements").value(1))
        .andExpect(jsonPath("$.data.reviews.content[0].bookingId").value(second.toString()));
  }

  @Test
  void simultaneousSubmissionsCreateOneReview() throws Exception {
    UUID id = booking("CHECKED_OUT");
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<Integer> call =
          () ->
              postReview(id, customer, 5, "Trải nghiệm tốt.").andReturn().getResponse().getStatus();
      var results = pool.invokeAll(List.of(call, call));
      assertThat(List.of(results.get(0).get(), results.get(1).get()))
          .containsExactlyInAnyOrder(201, 409);
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM guest_reviews WHERE booking_id=?", Long.class, id))
        .isEqualTo(1);
  }

  @Test
  void simultaneousRepliesRejectLostUpdate() throws Exception {
    UUID id = booking("CHECKED_OUT");
    postReview(id, customer, 5, "Tốt.").andExpect(status().isCreated());
    UUID review = review(id);
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<Integer> call =
          () ->
              reply(review, owner, "HOTEL_OWNER", 0, "Cảm ơn bạn.")
                  .andReturn()
                  .getResponse()
                  .getStatus();
      var results = pool.invokeAll(List.of(call, call));
      assertThat(List.of(results.get(0).get(), results.get(1).get()))
          .containsExactlyInAnyOrder(200, 409);
    }
    assertThat(
            jdbc.queryForObject("SELECT version FROM guest_reviews WHERE id=?", Long.class, review))
        .isEqualTo(1);
  }

  @Test
  void guestFieldsAreImmutableAndReplyValidationDoesNotMutateReview() throws Exception {
    UUID id = booking("CHECKED_OUT");
    postReview(id, customer, 2, "Cần cải thiện.").andExpect(status().isCreated());
    UUID review = review(id);
    reply(review, owner, "HOTEL_OWNER", 0, " ").andExpect(status().isBadRequest());
    reply(review, owner, "HOTEL_OWNER", 0, "a".repeat(2001)).andExpect(status().isBadRequest());
    assertThatThrownBy(() -> jdbc.update("UPDATE guest_reviews SET rating=5 WHERE id=?", review))
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT rating FROM guest_reviews WHERE id=?", Integer.class, review))
        .isEqualTo(2);
    assertThat(
            jdbc.queryForObject("SELECT version FROM guest_reviews WHERE id=?", Long.class, review))
        .isZero();
  }

  private UUID user(String role) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO users(id,email,password_hash,full_name,phone,status,email_verified_at) VALUES (?,?,'unused','Nguyễn Minh Anh',?,'active',now())",
        id,
        id + "@guest-review.test",
        "+" + (100000000000L + Math.abs((long) id.hashCode())));
    jdbc.update(
        "INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE role_key=?",
        id,
        role);
    return id;
  }

  private UUID booking(String state) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO bookings(id,booking_code,customer_id,hotel_id,booker_name_snapshot,booker_email_snapshot,booker_phone_snapshot,guest_name_snapshot,guest_email_snapshot,guest_phone_snapshot,check_in,check_out,days,nights,status) VALUES (?,?,?,?,'Khách','private@example.test','+84900000000','Khách','private@example.test','+84900000000','2027-01-01','2027-01-03',3,2,?::booking_status__t)",
        id,
        "GR-" + id.toString().substring(0, 12),
        customer,
        hotel,
        state);
    return id;
  }

  private UUID review(UUID booking) {
    return jdbc.queryForObject(
        "SELECT id FROM guest_reviews WHERE booking_id=?", UUID.class, booking);
  }

  private JwtRequestPostProcessor actor(UUID id, String role) {
    return jwt()
        .jwt(j -> j.subject(id.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  private ResultActions postReview(UUID id, UUID actor, int rating, String content)
      throws Exception {
    return mvc.perform(
        post("/api/v1/bookings/" + id + "/review")
            .with(actor(actor, "CUSTOMER"))
            .contentType("application/json")
            .content("{\"rating\":" + rating + ",\"content\":\"" + content + "\"}"));
  }

  private ResultActions reply(UUID id, UUID who, String role, long version, String content)
      throws Exception {
    return mvc.perform(
        put("/api/v1/management/hotels/" + hotel + "/guest-reviews/" + id + "/reply")
            .with(actor(who, role))
            .contentType("application/json")
            .content("{\"expectedVersion\":" + version + ",\"content\":\"" + content + "\"}"));
  }

  private ResultActions publicList() throws Exception {
    return mvc.perform(get("/api/v1/hotels/" + hotel + "/guest-reviews"));
  }

  private ResultActions managed(UUID who, String role) throws Exception {
    return mvc.perform(
        get("/api/v1/management/hotels/" + hotel + "/guest-reviews").with(actor(who, role)));
  }
}
