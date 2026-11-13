package com.team.hotelbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.team.hotelbooking.hotel.application.command.ApproveHotelReviewCommand;
import com.team.hotelbooking.hotel.application.usecase.ApproveHotelReviewUseCase;
import com.team.hotelbooking.hotel.domain.repository.HotelReviewRepository;
import com.team.hotelbooking.media.application.port.ObjectStorage;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
    properties = {
      "storage.r2.enabled=true",
      "storage.r2.endpoint=http://localhost:9999",
      "storage.r2.access-key-id=test",
      "storage.r2.secret-access-key=test",
      "storage.r2.bucket=reviews"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HotelReviewIntegrationTests {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper mapper;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired ApproveHotelReviewUseCase approveUseCase;
  @MockitoBean ObjectStorage storage;
  @MockitoSpyBean HotelReviewRepository reviews;

  UUID owner, admin, outsider, hotel, roomType, room, media;
  String management;

  @BeforeEach
  void setup() {
    owner = insertUser("HOTEL_OWNER");
    admin = insertUser("ADMIN");
    outsider = insertUser("HOTEL_OWNER");
    hotel = UUID.randomUUID();
    roomType = UUID.randomUUID();
    room = UUID.randomUUID();
    media = UUID.randomUUID();
    management = "/api/v1/management/hotels/" + hotel;
    jdbc.update(
        """
        INSERT INTO hotels (id, owner_user_id, city_id, name, slug, address, contact_phone)
        VALUES (?, ?, '822cafba-73f6-4ec0-bae7-71804535cae3', 'Review hotel', ?, '123 Beach', '+84912345678')
        """,
        hotel,
        owner,
        "review-" + hotel);
    jdbc.update(
        """
        INSERT INTO room_types (id, hotel_id, name, max_adults, max_children, max_occupancy, is_active)
        VALUES (?, ?, 'Deluxe', 2, 0, 2, true)
        """,
        roomType,
        hotel);
    jdbc.update(
        """
        INSERT INTO rooms (id, hotel_id, room_type_id, room_number, floor, operational_from)
        VALUES (?, ?, ?, '101', '1', '2026-01-01T00:00:00Z')
        """,
        room,
        hotel,
        roomType);
    jdbc.update(
        """
        INSERT INTO media_objects
          (id, storage_provider, bucket_name, object_key, original_filename, content_type,
           size_bytes, purpose, hotel_id, status, created_by_user_id)
        VALUES (?, 'R2', 'reviews', ?, 'hotel.jpg', 'image/jpeg', 1234, 'HOTEL_IMAGE', ?, 'READY', ?)
        """,
        media,
        "hotels/" + hotel + "/images/" + media + ".jpg",
        hotel,
        owner);
    jdbc.update(
        """
        INSERT INTO hotel_images (hotel_id, media_object_id, display_order, is_cover, alt_text)
        VALUES (?, ?, 0, true, 'Original photo')
        """,
        hotel,
        media);
    when(storage.createDownloadUrl(anyString(), any(Duration.class)))
        .thenReturn(URI.create("https://images.test/signed-photo"));
  }

  @Test
  void ownerSubmitsPersistedSnapshotAndOnlyAdminOrActualOwnerCanRead() throws Exception {
    String id = submit();
    mvc.perform(get("/api/v1/hotel-review-requests/" + id).with(as(owner, "HOTEL_OWNER")))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andExpect(jsonPath("$.data.submittedBy").value(owner.toString()))
        .andExpect(jsonPath("$.data.snapshot.hotel.name").value("Review hotel"))
        .andExpect(jsonPath("$.data.snapshot.roomTypes[0].roomType.name").value("Deluxe"))
        .andExpect(jsonPath("$.data.snapshot.rooms[0].roomNumber").value("101"))
        .andExpect(jsonPath("$.data.snapshot.images[0].mediaId").value(media.toString()));
    mvc.perform(get("/api/v1/hotel-review-requests/" + id).with(as(admin, "ADMIN")))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/hotel-review-requests/" + id).with(as(outsider, "HOTEL_OWNER")))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/hotel-review-requests/" + id)).andExpect(status().isUnauthorized());
    send(
            post(management + "/review-requests"),
            outsider,
            "HOTEL_OWNER",
            Map.of("expectedVersion", 0))
        .andExpect(status().isNotFound());
    send(post(management + "/review-requests"), admin, "ADMIN", Map.of("expectedVersion", 0))
        .andExpect(status().isForbidden());
  }

  @Test
  void validatesHotelVersionAndReadinessBeforeAcceptingSubmission() throws Exception {
    send(
            post(management + "/review-requests"),
            owner,
            "HOTEL_OWNER",
            Map.of("expectedVersion", 999))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("REVIEW_CONCURRENT_UPDATE"));
    jdbc.update("UPDATE hotels SET contact_phone = NULL WHERE id = ?", hotel);
    send(post(management + "/review-requests"), owner, "HOTEL_OWNER", Map.of("expectedVersion", 0))
        .andExpect(status().is(422))
        .andExpect(jsonPath("$.errors[0].field").value("contact"));
    assertThat(count("hotel_review_requests")).isZero();
    jdbc.update(
        "UPDATE hotels SET status = 'ACTIVE', contact_phone = '+84912345678' WHERE id = ?", hotel);
    send(post(management + "/review-requests"), owner, "HOTEL_OWNER", Map.of("expectedVersion", 0))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
  }

  @Test
  void concurrentSubmissionsProduceExactlyOnePendingReview() throws Exception {
    List<Integer> codes = race(() -> submissionStatus(), () -> submissionStatus());
    assertThat(codes).containsExactlyInAnyOrder(201, 409);
    assertThat(count("hotel_review_requests")).isEqualTo(1);
  }

  @Test
  void postgresUniqueIndexAlsoProtectsConcurrentInsertsOutsideTheService() throws Exception {
    // Dùng SQL trực tiếp để kiểm constraint, không dựa vào khóa ở application.
    Callable<Integer> insert =
        () -> {
          try {
            jdbc.update(
                """
            INSERT INTO hotel_review_requests (id, hotel_id, snapshot, submitted_by, submitted_at)
            VALUES (?, ?, '{}', ?, now())
            """,
                UUID.randomUUID(),
                hotel,
                owner);
            return 201;
          } catch (DataIntegrityViolationException expected) {
            return 409;
          }
        };
    assertThat(race(insert, insert)).containsExactlyInAnyOrder(201, 409);
    assertThat(count("hotel_review_requests")).isEqualTo(1);
  }

  @Test
  void pendingReviewLocksAllProfileWritesButKeepsOperationalRoomReadiness() throws Exception {
    submit();
    pending(send(patch(management), owner, "HOTEL_OWNER", Map.of("name", "Changed", "version", 0)));
    pending(
        send(
            post(management + "/room-types"),
            owner,
            "HOTEL_OWNER",
            Map.of("name", "Another", "maxAdults", 2, "maxChildren", 0)));
    pending(
        send(
            patch(management + "/room-types/" + roomType),
            owner,
            "HOTEL_OWNER",
            Map.of("name", "Changed")));
    pending(
        send(
            post(management + "/rooms"),
            owner,
            "HOTEL_OWNER",
            Map.of(
                "roomTypeId",
                roomType,
                "roomNumber",
                "102",
                "operationalFrom",
                "2026-01-01T00:00:00Z")));
    pending(
        send(
            patch(management + "/rooms/" + room),
            owner,
            "HOTEL_OWNER",
            Map.of("roomNumber", "Changed")));
    pending(
        send(
            put(management + "/amenities"), owner, "HOTEL_OWNER", Map.of("amenityIds", List.of())));
    pending(
        send(
            put(management + "/room-types/" + roomType + "/amenities"),
            owner,
            "HOTEL_OWNER",
            Map.of("amenityIds", List.of())));
    pending(send(post(management + "/amenities"), owner, "HOTEL_OWNER", Map.of("name", "Changed")));
    pending(
        send(
            put(management + "/images"),
            owner,
            "HOTEL_OWNER",
            Map.of("expectedVersion", 0, "images", List.of())));
    pending(
        send(
            post(management + "/image-albums"),
            owner,
            "HOTEL_OWNER",
            Map.of("expectedVersion", 0, "name", "Changed")));
    send(patch(management + "/rooms/" + room), owner, "HOTEL_OWNER", Map.of("status", "ready"))
        .andExpect(status().isOk());
    mvc.perform(get(management + "/amenities").with(as(admin, "ADMIN"))).andExpect(status().isOk());
  }

  @Test
  void adminCannotMutateOwnersHotelRoomTypesRoomsAmenitiesImagesOrAlbums() throws Exception {
    send(patch(management), admin, "ADMIN", Map.of("name", "Admin edit", "version", 0))
        .andExpect(status().isForbidden());
    send(
            post(management + "/room-types"),
            admin,
            "ADMIN",
            Map.of("name", "Admin edit", "maxAdults", 2, "maxChildren", 0))
        .andExpect(status().isForbidden());
    send(
            patch(management + "/room-types/" + roomType),
            admin,
            "ADMIN",
            Map.of("name", "Admin edit"))
        .andExpect(status().isForbidden());
    send(
            post(management + "/rooms"),
            admin,
            "ADMIN",
            Map.of(
                "roomTypeId",
                roomType,
                "roomNumber",
                "102",
                "operationalFrom",
                "2026-01-01T00:00:00Z"))
        .andExpect(status().isForbidden());
    send(patch(management + "/rooms/" + room), admin, "ADMIN", Map.of("floor", 2))
        .andExpect(status().isForbidden());
    send(put(management + "/amenities"), admin, "ADMIN", Map.of("amenityIds", List.of()))
        .andExpect(status().isForbidden());
    send(post(management + "/amenities"), admin, "ADMIN", Map.of("name", "Admin edit"))
        .andExpect(status().isForbidden());
    send(
            put(management + "/images"),
            admin,
            "ADMIN",
            Map.of("expectedVersion", 0, "images", List.of()))
        .andExpect(status().isForbidden());
    send(
            post(management + "/image-albums"),
            admin,
            "ADMIN",
            Map.of("expectedVersion", 0, "name", "Admin edit"))
        .andExpect(status().isForbidden());
    assertThat(hotelStatus()).isEqualTo("DRAFT");
  }

  @Test
  void approveChangesReviewHotelAndHistoryTogetherAndChecksVersions() throws Exception {
    String id = submit();
    decision(id, "approve", admin, "ADMIN", Map.of("expectedVersion", 99))
        .andExpect(status().isConflict());
    decision(id, "approve", owner, "HOTEL_OWNER", Map.of("expectedVersion", 0))
        .andExpect(status().isForbidden());
    decision(id, "approve", admin, "ADMIN", Map.of("expectedVersion", 0))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("APPROVED"))
        .andExpect(jsonPath("$.data.version").value(1))
        .andExpect(jsonPath("$.data.reviewedBy").value(admin.toString()))
        .andExpect(jsonPath("$.data.reviewedAt").isNotEmpty());
    assertThat(hotelStatus()).isEqualTo("ACTIVE");
    assertThat(count("hotel_status_history")).isEqualTo(1);
    decision(id, "reject", admin, "ADMIN", Map.of("expectedVersion", 1, "reason", "Too late"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("REVIEW_ALREADY_DECIDED"));
  }

  @Test
  void rejectRequiresReasonUnlocksEditsAndResubmissionKeepsOldSnapshotAndImages() throws Exception {
    String first = submit();
    decision(first, "reject", admin, "ADMIN", Map.of("expectedVersion", 0, "reason", "   "))
        .andExpect(status().isBadRequest());
    decision(first, "reject", admin, "ADMIN", Map.of("expectedVersion", 99, "reason", "Missing"))
        .andExpect(status().isConflict());
    decision(
            first,
            "reject",
            admin,
            "ADMIN",
            Map.of("expectedVersion", 0, "reason", "  Please improve  "))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reason").value("Please improve"));
    assertThat(hotelStatus()).isEqualTo("DRAFT");
    send(patch(management), owner, "HOTEL_OWNER", Map.of("version", 0, "name", "Revised hotel"))
        .andExpect(status().isOk());
    send(
            put(management + "/images"),
            owner,
            "HOTEL_OWNER",
            Map.of("expectedVersion", hotelVersion(), "images", List.of()))
        .andExpect(status().isOk());
    String second = submit();
    assertThat(second).isNotEqualTo(first);
    assertThat(count("hotel_review_requests")).isEqualTo(2);
    mvc.perform(get("/api/v1/hotel-review-requests/" + first).with(as(admin, "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.snapshot.hotel.name").value("Review hotel"))
        .andExpect(jsonPath("$.data.snapshot.images[0].mediaId").value(media.toString()));
    mvc.perform(get("/api/v1/hotel-review-requests/" + second).with(as(admin, "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.snapshot.hotel.name").value("Revised hotel"))
        .andExpect(jsonPath("$.data.snapshot.images").isEmpty());
    // Ảnh cũ vẫn xem được qua API access mặc dù đã bỏ khỏi danh sách ảnh hiện tại.
    mvc.perform(get("/api/v1/media/" + media + "/access").with(as(admin, "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.url").value("https://images.test/signed-photo"));
    mvc.perform(get("/api/v1/media/" + media + "/access").with(as(outsider, "HOTEL_OWNER")))
        .andExpect(status().isNotFound());
    assertThrows(
        DataIntegrityViolationException.class,
        () -> jdbc.update("DELETE FROM media_objects WHERE id = ?", media));
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbc.update(
                "UPDATE media_objects SET status = 'DELETED', deleted_at = now() WHERE id = ?",
                media));
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbc.update(
                "UPDATE hotel_review_requests SET snapshot = '{}' WHERE id = ?",
                UUID.fromString(first)));
  }

  @Test
  void concurrentApproveAndRejectCommitOnlyOneDecision() throws Exception {
    String id = submit();
    assertThat(
            race(
                () ->
                    decision(id, "approve", admin, "ADMIN", Map.of("expectedVersion", 0))
                        .andReturn()
                        .getResponse()
                        .getStatus(),
                () ->
                    decision(
                            id,
                            "reject",
                            admin,
                            "ADMIN",
                            Map.of("expectedVersion", 0, "reason", "Rejected"))
                        .andReturn()
                        .getResponse()
                        .getStatus()))
        .containsExactlyInAnyOrder(200, 409);
    String reviewStatus =
        jdbc.queryForObject(
            "SELECT status FROM hotel_review_requests WHERE id = ?",
            String.class,
            UUID.fromString(id));
    assertThat(hotelStatus()).isEqualTo(reviewStatus.equals("APPROVED") ? "ACTIVE" : "DRAFT");
    assertThat(count("hotel_status_history")).isEqualTo(reviewStatus.equals("APPROVED") ? 1 : 0);
  }

  @Test
  void decisionWriteFailureRollsBackHotelAndHistoryAlreadyFlushed() throws Exception {
    String id = submit();
    doThrow(new IllegalStateException("Simulated decision write failure"))
        .when(reviews)
        .updateDecision(any());
    assertThrows(
        org.springframework.dao.InvalidDataAccessApiUsageException.class,
        () ->
            approveUseCase.execute(new ApproveHotelReviewCommand(admin, UUID.fromString(id), 0L)));
    assertThat(hotelStatus()).isEqualTo("DRAFT");
    assertThat(count("hotel_status_history")).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM hotel_review_requests WHERE id = ?",
                String.class,
                UUID.fromString(id)))
        .isEqualTo("PENDING");
  }

  @Test
  void approvalRechecksReadinessAndKeepsReviewPendingOnFailure() throws Exception {
    String id = submit();
    jdbc.update("UPDATE rooms SET operational_until = now() WHERE id = ?", room);
    decision(id, "approve", admin, "ADMIN", Map.of("expectedVersion", 0))
        .andExpect(status().is(422));
    assertThat(hotelStatus()).isEqualTo("DRAFT");
    assertThat(count("hotel_status_history")).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM hotel_review_requests WHERE id = ?",
                String.class,
                UUID.fromString(id)))
        .isEqualTo("PENDING");
  }

  @Test
  void legacyApproveRequiresPendingReviewAndUsesTheSameDecisionFlow() throws Exception {
    mvc.perform(patch(management + "/approve").with(as(admin, "ADMIN")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("REVIEW_REQUIRED"));
    String id = submit();
    mvc.perform(patch(management + "/approve").with(as(admin, "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM hotel_review_requests WHERE id = ?",
                String.class,
                UUID.fromString(id)))
        .isEqualTo("APPROVED");
    mvc.perform(patch(management + "/approve").with(as(admin, "ADMIN")))
        .andExpect(status().isConflict());
  }

  @Test
  void adminListsFilteredSummariesWithStablePagination() throws Exception {
    String first = submit();
    decision(
            first, "reject", admin, "ADMIN", Map.of("expectedVersion", 0, "reason", "Review again"))
        .andExpect(status().isOk());
    String second = submit();
    mvc.perform(
            get("/api/v1/admin/hotel-review-requests")
                .with(as(admin, "ADMIN"))
                .param("hotelId", hotel.toString())
                .param("purpose", "HOTEL_IMAGE")
                .param("page", "0")
                .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$.data.content[0].id").value(second))
        .andExpect(jsonPath("$.data.content[0].snapshot").doesNotExist());
    mvc.perform(
            get("/api/v1/admin/hotel-review-requests")
                .with(as(admin, "ADMIN"))
                .param("hotelId", hotel.toString())
                .param("purpose", "HOTEL_IMAGE")
                .param("status", "REJECTED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].id").value(first));
    mvc.perform(get("/api/v1/admin/hotel-review-requests").with(as(owner, "HOTEL_OWNER")))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/v1/admin/hotel-review-requests")
                .with(as(admin, "ADMIN"))
                .param("status", "INVALID"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void submissionWaitsForConcurrentContentEditAndSnapshotsCommittedData() throws Exception {
    var locked = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var edit =
          pool.submit(
              () ->
                  new TransactionTemplate(transactionManager)
                      .executeWithoutResult(
                          tx -> {
                            jdbc.queryForObject(
                                "SELECT id FROM hotels WHERE id = ? FOR UPDATE", UUID.class, hotel);
                            jdbc.update(
                                "UPDATE hotels SET name = 'Committed edit', version = version + 1 WHERE id = ?",
                                hotel);
                            locked.countDown();
                            await(release);
                          }));
      assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
      var submission =
          pool.submit(
              () ->
                  send(
                      post(management + "/review-requests"),
                      owner,
                      "HOTEL_OWNER",
                      Map.of("expectedVersion", 1)));
      try {
        // Chưa commit edit: parent lock không cho gửi hồ sơ đọc dữ liệu chưa đồng bộ.
        assertThrows(
            java.util.concurrent.TimeoutException.class,
            () -> submission.get(200, TimeUnit.MILLISECONDS));
      } finally {
        release.countDown();
      }
      edit.get(10, TimeUnit.SECONDS);
      submission
          .get(10, TimeUnit.SECONDS)
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.snapshot.hotel.name").value("Committed edit"));
    } finally {
      release.countDown();
    }
  }

  private int submissionStatus() throws Exception {
    return send(
            post(management + "/review-requests"),
            owner,
            "HOTEL_OWNER",
            Map.of("expectedVersion", 0))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  @Test
  void ownerCanReloadLatestReviewAndHistoryOnlyForOwnHotel() throws Exception {
    String path = management + "/review-requests";
    mvc.perform(get(path).with(as(owner, "HOTEL_OWNER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(0));
    String first = submit();
    decision(
            first,
            "reject",
            admin,
            "ADMIN",
            Map.of("expectedVersion", 0, "reason", "Improve details"))
        .andExpect(status().isOk());
    String second = submit();
    mvc.perform(get(path).with(as(owner, "HOTEL_OWNER")).param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$.data.content[0].id").value(second))
        .andExpect(jsonPath("$.data.content[0].status").value("PENDING"))
        .andExpect(jsonPath("$.data.content[0].hotelName").value("Review hotel"));
    mvc.perform(get(path).with(as(owner, "HOTEL_OWNER")).param("size", "1").param("page", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].id").value(first))
        .andExpect(jsonPath("$.data.content[0].reason").value("Improve details"));
    mvc.perform(get(path).with(as(admin, "ADMIN"))).andExpect(status().isOk());
    mvc.perform(get(path).with(as(outsider, "HOTEL_OWNER"))).andExpect(status().isNotFound());
    mvc.perform(get(path)).andExpect(status().isUnauthorized());
  }

  @Test
  void uploadCannotBeUsedToEditAsAdminOrDuringReview() throws Exception {
    var file =
        new org.springframework.mock.web.MockMultipartFile(
            "file", "photo.jpg", "image/jpeg", new byte[] {1, 2, 3});
    mvc.perform(
            multipart("/api/v1/media")
                .file(file)
                .param("hotelId", hotel.toString())
                .param("purpose", "HOTEL_IMAGE")
                .with(as(admin, "ADMIN")))
        .andExpect(status().isForbidden());
    submit();
    mvc.perform(
            multipart("/api/v1/media")
                .file(file)
                .param("hotelId", hotel.toString())
                .param("purpose", "HOTEL_IMAGE")
                .with(as(owner, "HOTEL_OWNER")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("REVIEW_PENDING"));
  }

  @Test
  void snapshotRetainsAmenitiesRoomContentAndAlbumMetadataAfterRejectionAndEdits()
      throws Exception {
    var albumResponse =
        send(
                post(management + "/image-albums"),
                owner,
                "HOTEL_OWNER",
                Map.of("expectedVersion", hotelVersion(), "name", "Original album"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    String album =
        mapper.readTree(albumResponse.getContentAsString()).get("data").get("id").asString();
    send(
            put(management + "/images"),
            owner,
            "HOTEL_OWNER",
            Map.of(
                "expectedVersion",
                hotelVersion(),
                "images",
                List.of(Map.of("mediaId", media, "albumId", album, "sortOrder", 0, "cover", true))))
        .andExpect(status().isOk());
    send(post(management + "/amenities"), owner, "HOTEL_OWNER", Map.of("name", "Private pool"))
        .andExpect(status().isCreated());
    send(
            post(management + "/rooms/" + room + "/amenities"),
            owner,
            "HOTEL_OWNER",
            Map.of("name", "Room sauna"))
        .andExpect(status().isCreated());
    send(
            put(management + "/room-types/" + roomType + "/amenities"),
            owner,
            "HOTEL_OWNER",
            Map.of("amenityIds", List.of("4e10ab03-f280-4717-a8ef-0155f186c41a")))
        .andExpect(status().isOk());
    String id = submit();
    JsonNode before =
        mapper
            .readTree(
                mvc.perform(get("/api/v1/hotel-review-requests/" + id).with(as(admin, "ADMIN")))
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("data")
            .get("snapshot");
    assertThat(before.get("albums").get(0).get("name").asString()).isEqualTo("Original album");
    assertThat(before.get("images").get(0).get("albumId").asString()).isEqualTo(album);
    assertThat(before.get("customAmenities").get(0).get("name").asString())
        .isEqualTo("Private pool");
    assertThat(before.get("rooms").get(0).get("customAmenities").get(0).get("name").asString())
        .isEqualTo("Room sauna");
    assertThat(before.get("roomTypes").get(0).get("amenities").get(0).get("code").asString())
        .isEqualTo("FREE_WIFI");
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbc.update(
                "UPDATE hotel_review_requests SET snapshot = '{}' WHERE id = ?",
                UUID.fromString(id)));
    decision(id, "reject", admin, "ADMIN", Map.of("expectedVersion", 0, "reason", "Please revise"))
        .andExpect(status().isOk());
    send(
            patch(management + "/image-albums/" + album),
            owner,
            "HOTEL_OWNER",
            Map.of("expectedVersion", hotelVersion(), "name", "Revised album"))
        .andExpect(status().isOk());
    send(
            patch(management + "/room-types/" + roomType),
            owner,
            "HOTEL_OWNER",
            Map.of("name", "Revised room type"))
        .andExpect(status().isOk());
    send(patch(management + "/rooms/" + room), owner, "HOTEL_OWNER", Map.of("roomNumber", "202"))
        .andExpect(status().isOk());
    JsonNode after =
        mapper
            .readTree(
                mvc.perform(get("/api/v1/hotel-review-requests/" + id).with(as(admin, "ADMIN")))
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("data")
            .get("snapshot");
    assertThat(after).isEqualTo(before);
  }

  private String submit() throws Exception {
    var response =
        send(
                post(management + "/review-requests"),
                owner,
                "HOTEL_OWNER",
                Map.of("expectedVersion", hotelVersion()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    JsonNode data = mapper.readTree(response.getContentAsString()).get("data");
    assertThat(response.getHeader("Location")).endsWith(data.get("id").asString());
    return data.get("id").asString();
  }

  private ResultActions decision(String id, String action, UUID actor, String role, Object body)
      throws Exception {
    return send(
        post("/api/v1/admin/hotel-review-requests/" + id + "/" + action), actor, role, body);
  }

  private ResultActions send(
      MockHttpServletRequestBuilder request, UUID actor, String role, Object body)
      throws Exception {
    return mvc.perform(
        request
            .with(as(actor, role))
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(body)));
  }

  private static void pending(ResultActions result) throws Exception {
    result.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("REVIEW_PENDING"));
  }

  private static JwtRequestPostProcessor as(UUID id, String role) {
    return jwt()
        .jwt(token -> token.subject(id.toString()))
        .authorities(new SimpleGrantedAuthority("ROLE_" + role));
  }

  private UUID insertUser(String role) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO users (id, email, password_hash, full_name, phone, status, email_verified_at)
        VALUES (?, ?, 'test-hash', 'Hotel review tester', ?, 'active', now())
        """,
        id,
        "review-" + id + "@test.local",
        String.format("+84%012d", Math.floorMod(id.getMostSignificantBits(), 1_000_000_000_000L)));
    jdbc.update(
        "INSERT INTO user_roles (user_id, role_id) SELECT ?, id FROM roles WHERE role_key = ?",
        id,
        role);
    return id;
  }

  private String hotelStatus() {
    return jdbc.queryForObject("SELECT status::text FROM hotels WHERE id = ?", String.class, hotel);
  }

  private long hotelVersion() {
    return jdbc.queryForObject("SELECT version FROM hotels WHERE id = ?", Long.class, hotel);
  }

  private long count(String table) {
    // table chỉ là hằng trong test, hotel là bind parameter.
    return jdbc.queryForObject(
        "SELECT count(*) FROM " + table + " WHERE hotel_id = ?", Long.class, hotel);
  }

  private static List<Integer> race(Callable<Integer> first, Callable<Integer> second)
      throws Exception {
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var futures =
          List.of(first, second).stream()
              .map(
                  work ->
                      pool.submit(
                          () -> {
                            ready.countDown();
                            await(start);
                            return work.call();
                          }))
              .toList();
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      return List.of(
          futures.get(0).get(20, TimeUnit.SECONDS), futures.get(1).get(20, TimeUnit.SECONDS));
    } finally {
      start.countDown();
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS))
        throw new IllegalStateException("Timed out waiting for test barrier");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }
}
