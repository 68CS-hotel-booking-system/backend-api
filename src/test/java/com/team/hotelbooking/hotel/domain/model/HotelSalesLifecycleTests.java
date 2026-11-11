package com.team.hotelbooking.hotel.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.team.hotelbooking.hotel.domain.exception.HotelErrorCode;
import com.team.hotelbooking.hotel.domain.exception.HotelException;
import com.team.hotelbooking.hotel.domain.service.HotelPublishReadiness;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Vòng đời bán khách sạn (HT-03, HT-05, HT-09)")
class HotelSalesLifecycleTests {

  private static final UUID HOTEL_ID = UUID.fromString("7b4f2c1e-3d5a-4e6b-8c9d-0a1b2c3d4e5f");
  private static final UUID ACTOR_ID = UUID.fromString("4c5d6e7f-8a9b-4c0d-9e2f-3a4b5c6d7e8f");
  private static final Instant CREATED = Instant.parse("2026-11-01T00:00:00Z");
  private static final Instant NOW = Instant.parse("2026-11-09T08:00:00Z");

  private static Hotel hotel(Hotel.Status status, String phone, String email) {
    return new Hotel(
        HOTEL_ID,
        UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9"),
        UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3"),
        "Khách sạn Biển Xanh",
        "khach-san-bien-xanh",
        null,
        "12 Võ Nguyên Giáp",
        phone,
        email,
        null,
        null,
        null,
        LocalTime.of(14, 0),
        LocalTime.of(12, 0),
        status,
        CREATED,
        CREATED,
        3L);
  }

  private static Hotel hotel(Hotel.Status status) {
    return hotel(status, "+84905123456", null);
  }

  private static RoomType roomType(UUID id, boolean active, short adults, short occupancy) {
    return new RoomType(
        id, HOTEL_ID, "Deluxe", null, adults, (short) 0, occupancy, 0L, active, CREATED, CREATED,
        0L);
  }

  private static HotelErrorCode codeOf(Throwable exception) {
    return ((HotelException) exception).code();
  }

  @Nested
  @DisplayName("Chuyển trạng thái")
  class TransitionTests {

    @Test
    @DisplayName("activate (approve) chuyển DRAFT sang ACTIVE, giữ nội dung, liên hệ và version")
    void shouldMoveDraftToActiveWhenActivating() {
      // When
      Hotel published = hotel(Hotel.Status.DRAFT).activate(NOW);

      // Then
      assertThat(published.status()).isEqualTo(Hotel.Status.ACTIVE);
      assertThat(published.updatedAt()).isEqualTo(NOW);
      assertThat(published.version()).isEqualTo(3L);
      assertThat(published.contactPhone()).isEqualTo("+84905123456");
      assertThat(published.onSale()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(
        value = Hotel.Status.class,
        names = {"ACTIVE", "SUSPENDED", "LOCKED"})
    @DisplayName("activate giữ lỗi VALIDATION_FAILED của develop với mọi trạng thái khác DRAFT")
    void shouldRejectActivateWhenHotelIsNotDraft(Hotel.Status status) {
      assertThatThrownBy(() -> hotel(status).activate(NOW))
          .satisfies(ex -> assertThat(codeOf(ex)).isEqualTo(HotelErrorCode.VALIDATION_FAILED));
    }

    @Test
    @DisplayName("suspend chỉ từ ACTIVE; resume đưa SUSPENDED về ACTIVE")
    void shouldSuspendActiveHotelAndResumeIt() {
      // When
      Hotel suspended = hotel(Hotel.Status.ACTIVE).suspend(NOW);
      Hotel resumed = suspended.resume(NOW.plusSeconds(60));

      // Then
      assertThat(suspended.status()).isEqualTo(Hotel.Status.SUSPENDED);
      assertThat(suspended.onSale()).isFalse();
      assertThat(resumed.status()).isEqualTo(Hotel.Status.ACTIVE);
    }

    @Test
    @DisplayName("Quản lý không duyệt lần đầu qua resume khi khách sạn còn DRAFT")
    void shouldRejectResumeWhenHotelIsDraft() {
      assertThatThrownBy(() -> hotel(Hotel.Status.DRAFT).resume(NOW))
          .hasMessage(
              "Khách sạn chưa được công khai lần đầu; chỉ quản trị hệ thống được công khai.")
          .satisfies(
              ex -> assertThat(codeOf(ex)).isEqualTo(HotelErrorCode.INVALID_STATUS_TRANSITION));
    }

    @Test
    @DisplayName("suspend/resume khách sạn LOCKED trả HOTEL_LOCKED")
    void shouldRejectManagerTransitionsWhenHotelIsLocked() {
      Hotel locked = hotel(Hotel.Status.LOCKED);

      assertThatThrownBy(() -> locked.suspend(NOW))
          .satisfies(ex -> assertThat(codeOf(ex)).isEqualTo(HotelErrorCode.HOTEL_LOCKED));
      assertThatThrownBy(() -> locked.resume(NOW))
          .satisfies(ex -> assertThat(codeOf(ex)).isEqualTo(HotelErrorCode.HOTEL_LOCKED));
    }

    @Test
    @DisplayName("suspend từ chối khách sạn đã SUSPENDED")
    void shouldRejectSuspendWhenHotelIsAlreadySuspended() {
      assertThatThrownBy(() -> hotel(Hotel.Status.SUSPENDED).suspend(NOW))
          .satisfies(
              ex -> assertThat(codeOf(ex)).isEqualTo(HotelErrorCode.INVALID_STATUS_TRANSITION));
    }

    @ParameterizedTest
    @EnumSource(
        value = Hotel.Status.class,
        names = {"DRAFT", "ACTIVE", "SUSPENDED"})
    @DisplayName("lock khóa được mọi trạng thái chưa khóa")
    void shouldLockHotelFromAnyUnlockedStatus(Hotel.Status status) {
      assertThat(hotel(status).lock(NOW).status()).isEqualTo(Hotel.Status.LOCKED);
    }

    @Test
    @DisplayName("lock từ chối khách sạn đã LOCKED")
    void shouldRejectLockWhenHotelIsAlreadyLocked() {
      assertThatThrownBy(() -> hotel(Hotel.Status.LOCKED).lock(NOW))
          .satisfies(
              ex -> assertThat(codeOf(ex)).isEqualTo(HotelErrorCode.INVALID_STATUS_TRANSITION));
    }

    @ParameterizedTest
    @EnumSource(
        value = Hotel.Status.class,
        names = {"ACTIVE", "SUSPENDED"})
    @DisplayName("unlock đưa khách sạn từng công khai về SUSPENDED, không mở bán ngay")
    void shouldUnlockToSuspendedWhenHotelWasPublishedBeforeLock(Hotel.Status beforeLock) {
      assertThat(hotel(Hotel.Status.LOCKED).unlock(beforeLock, NOW).status())
          .isEqualTo(Hotel.Status.SUSPENDED);
    }

    @Test
    @DisplayName("unlock khách sạn bị khóa khi còn DRAFT thì về DRAFT")
    void shouldUnlockToDraftWhenHotelWasLockedBeforeFirstPublish() {
      assertThat(hotel(Hotel.Status.LOCKED).unlock(Hotel.Status.DRAFT, NOW).status())
          .isEqualTo(Hotel.Status.DRAFT);
    }

    @Test
    @DisplayName("unlock từ chối khách sạn không bị khóa")
    void shouldRejectUnlockWhenHotelIsNotLocked() {
      assertThatThrownBy(() -> hotel(Hotel.Status.ACTIVE).unlock(Hotel.Status.ACTIVE, NOW))
          .satisfies(
              ex -> assertThat(codeOf(ex)).isEqualTo(HotelErrorCode.INVALID_STATUS_TRANSITION));
    }
  }

  @Nested
  @DisplayName("Điều kiện mở bán HT-03")
  class ReadinessTests {

    private final UUID roomTypeId = UUID.randomUUID();

    @Test
    @DisplayName("Đủ điều kiện khi có liên hệ và loại phòng hợp lệ có phòng đang phục vụ")
    void shouldHaveNoUnmetRequirementWhenHotelIsComplete() {
      assertThat(
              HotelPublishReadiness.unmetRequirements(
                  hotel(Hotel.Status.DRAFT),
                  List.of(roomType(roomTypeId, true, (short) 2, (short) 2)),
                  Map.of(roomTypeId, 1L)))
          .isEmpty();
    }

    @Test
    @DisplayName("Liệt kê cùng lúc thiếu liên hệ và thiếu loại phòng hợp lệ")
    void shouldListAllUnmetRequirementsWhenContactAndRoomTypeAreMissing() {
      assertThat(
              HotelPublishReadiness.unmetRequirements(
                  hotel(Hotel.Status.DRAFT, null, null),
                  List.of(
                      roomType(roomTypeId, false, (short) 2, (short) 2),
                      roomType(UUID.randomUUID(), true, (short) 3, (short) 2)),
                  Map.of(roomTypeId, 5L)))
          .containsExactly(HotelPublishRequirement.CONTACT, HotelPublishRequirement.ROOM_TYPE);
    }

    @Test
    @DisplayName("Thiếu phòng đang phục vụ khi loại phòng hợp lệ chưa có phòng vật lý")
    void shouldRequireOperationalRoomWhenSellableRoomTypeHasNoRoom() {
      UUID otherRoomType = UUID.randomUUID();

      assertThat(
              HotelPublishReadiness.unmetRequirements(
                  hotel(Hotel.Status.DRAFT, null, "lienhe@bienxanh.vn"),
                  List.of(
                      roomType(roomTypeId, true, (short) 2, (short) 2),
                      roomType(otherRoomType, false, (short) 2, (short) 2)),
                  Map.of(otherRoomType, 3L)))
          .containsExactly(HotelPublishRequirement.OPERATIONAL_ROOM);
    }
  }

  @Nested
  @DisplayName("Lịch sử đổi trạng thái")
  class StatusChangeTests {

    @Test
    @DisplayName("Ghi from/to, người thực hiện, thời điểm và lý do đã cắt khoảng trắng")
    void shouldCaptureTransitionWhenRecordingStatusChange() {
      // Given
      Hotel before = hotel(Hotel.Status.ACTIVE);
      Hotel after = before.lock(NOW);
      UUID id = UUID.randomUUID();

      // When
      HotelStatusChange change =
          HotelStatusChange.of(id, before, after, "  Vi phạm quy định  ", true, ACTOR_ID);

      // Then
      assertThat(change)
          .isEqualTo(
              new HotelStatusChange(
                  id,
                  HOTEL_ID,
                  Hotel.Status.ACTIVE,
                  Hotel.Status.LOCKED,
                  "Vi phạm quy định",
                  ACTOR_ID,
                  NOW));
    }

    @Test
    @DisplayName("Bắt buộc lý do khi khóa/gỡ khóa; lý do tùy chọn thì khoảng trắng thành null")
    void shouldRequireReasonOnlyWhenFlagIsSet() {
      Hotel before = hotel(Hotel.Status.ACTIVE);

      assertThatThrownBy(
              () ->
                  HotelStatusChange.of(
                      UUID.randomUUID(), before, before.lock(NOW), "   ", true, ACTOR_ID))
          .hasMessage("Phải nhập lý do khóa hoặc gỡ khóa bán.")
          .satisfies(ex -> assertThat(codeOf(ex)).isEqualTo(HotelErrorCode.VALIDATION_FAILED));
      assertThat(
              HotelStatusChange.of(
                      UUID.randomUUID(), before, before.suspend(NOW), "   ", false, ACTOR_ID)
                  .reason())
          .isNull();
    }

    @Test
    @DisplayName("Từ chối lý do dài hơn 500 ký tự")
    void shouldRejectReasonLongerThanLimit() {
      Hotel before = hotel(Hotel.Status.ACTIVE);

      assertThatThrownBy(
              () ->
                  HotelStatusChange.of(
                      UUID.randomUUID(),
                      before,
                      before.suspend(NOW),
                      "x".repeat(501),
                      false,
                      ACTOR_ID))
          .satisfies(ex -> assertThat(codeOf(ex)).isEqualTo(HotelErrorCode.VALIDATION_FAILED));
    }
  }

  @Nested
  @DisplayName("Liên hệ khách sạn khi PATCH")
  class ContactTests {

    @Test
    @DisplayName("Cắt khoảng trắng và giữ liên hệ cũ khi field không gửi")
    void shouldNormalizeContactAndKeepOtherWhenOnlyEmailIsSent() {
      // When
      Hotel updated =
          hotel(Hotel.Status.DRAFT)
              .updateDetails(
                  null,
                  null,
                  null,
                  null,
                  "  lienhe@bienxanh.vn ",
                  null,
                  null,
                  null,
                  null,
                  null,
                  NOW);

      // Then
      assertThat(updated.contactEmail()).isEqualTo("lienhe@bienxanh.vn");
      assertThat(updated.contactPhone()).isEqualTo("+84905123456");
    }

    @Test
    @DisplayName("Từ chối số điện thoại sai định dạng và email không hợp lệ")
    void shouldRejectMalformedContact() {
      Hotel hotel = hotel(Hotel.Status.DRAFT);

      assertThatThrownBy(
              () ->
                  hotel.updateDetails(
                      null, null, null, "0905-123", null, null, null, null, null, null, NOW))
          .satisfies(ex -> assertThat(codeOf(ex)).isEqualTo(HotelErrorCode.VALIDATION_FAILED));
      assertThatThrownBy(
              () ->
                  hotel.updateDetails(
                      null,
                      null,
                      null,
                      null,
                      "khong-phai-email",
                      null,
                      null,
                      null,
                      null,
                      null,
                      NOW))
          .hasMessage("Email liên hệ không hợp lệ.");
    }
  }
}
