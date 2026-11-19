package com.team.hotelbooking.pricing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.team.hotelbooking.pricing.application.command.GetRoomRatesQuery;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort.HotelManagement;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort.HotelStatus;
import com.team.hotelbooking.pricing.application.port.PricingHotelPort.Level;
import com.team.hotelbooking.pricing.application.result.RoomTypeRatesResult;
import com.team.hotelbooking.pricing.domain.exception.PricingErrorCode;
import com.team.hotelbooking.pricing.domain.exception.PricingException;
import com.team.hotelbooking.pricing.domain.model.RoomRate;
import com.team.hotelbooking.pricing.domain.repository.RoomRateRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dịch vụ tra cứu giá phòng theo ngày")
class GetRoomRatesServiceTests {

  private static final UUID ACTOR_ID = UUID.fromString("1f2e3d4c-5b6a-4978-8695-a4b3c2d1e0f9");
  private final UUID hotelId = UUID.randomUUID();
  private final UUID roomTypeId = UUID.randomUUID();

  @Mock private PricingHotelPort pricingHotelPort;
  @Mock private RoomRateRepository roomRateRepository;

  private GetRoomRatesService service;

  @BeforeEach
  void setUp() {
    service = new GetRoomRatesService(pricingHotelPort, roomRateRepository);
  }

  private void givenAccess(UUID hotelId, Level level, HotelStatus status) {
    given(pricingHotelPort.findManagementAccess(hotelId, ACTOR_ID, false))
        .willReturn(Optional.of(new HotelManagement(level, status)));
  }

  private void givenRoomType(UUID hotelId, UUID roomTypeId, long basePrice) {
    given(pricingHotelPort.findActiveRoomType(roomTypeId, hotelId))
        .willReturn(
            Optional.of(
                new PricingHotelPort.RoomTypePricingSnapshot(
                    roomTypeId, hotelId, "Deluxe", 2, 1, 3, basePrice)));
  }

  @Test
  @DisplayName("Lưới 7 ngày dùng giá cơ bản và hai giá riêng, đọc lại phản ánh giá cơ bản mới")
  void shouldResolveSevenDayGridAndReflectBasePriceChanges() {
    LocalDate start = LocalDate.of(2026, 11, 12);
    LocalDate end = start.plusDays(6);
    Instant updatedAt = Instant.parse("2026-11-12T00:00:00Z");
    givenAccess(hotelId, Level.OWNER, HotelStatus.DRAFT);
    givenRoomType(hotelId, roomTypeId, 500_000L);
    given(roomRateRepository.findRatesByRoomTypeIdAndDateRange(roomTypeId, start, end))
        .willReturn(
            List.of(
                new RoomRate(
                    roomTypeId, start.plusDays(1), 700_000L, "VND", ACTOR_ID, updatedAt, updatedAt),
                new RoomRate(
                    roomTypeId,
                    start.plusDays(2),
                    700_000L,
                    "VND",
                    ACTOR_ID,
                    updatedAt,
                    updatedAt)));
    var query = new GetRoomRatesQuery(ACTOR_ID, false, hotelId, roomTypeId, start, end);

    var first = service.execute(query);

    assertThat(first.rates())
        .extracting(r -> r.effectivePrice())
        .containsExactly(500_000L, 700_000L, 700_000L, 500_000L, 500_000L, 500_000L, 500_000L);
    assertThat(first.rates())
        .extracting(r -> r.source())
        .containsExactly("BASE", "RATE", "RATE", "BASE", "BASE", "BASE", "BASE");
    assertThat(first.rates().get(0).price()).isNull();
    assertThat(first.rates().get(0).hasRate()).isFalse();
    assertThat(first.rates().get(0).updatedAt()).isNull();
    assertThat(first.rates().get(1).price()).isEqualTo(700_000L);
    assertThat(first.rates().get(1).updatedAt()).isEqualTo(updatedAt);

    givenRoomType(hotelId, roomTypeId, 600_000L);
    assertThat(service.execute(query).rates())
        .extracting(r -> r.effectivePrice())
        .containsExactly(600_000L, 700_000L, 700_000L, 600_000L, 600_000L, 600_000L, 600_000L);
  }

  @Test
  @DisplayName("Trả đủ lịch dù thiếu giá; giá riêng không hợp lệ không dùng giá gốc thay thế")
  void shouldReturnAllDaysIncludingUnpricedAndInvalidOverrides() {
    LocalDate start = LocalDate.of(2020, 1, 1);
    LocalDate end = start.plusDays(2);
    givenAccess(hotelId, Level.OWNER, HotelStatus.ACTIVE);
    givenRoomType(hotelId, roomTypeId, 0L);
    given(roomRateRepository.findRatesByRoomTypeIdAndDateRange(roomTypeId, start, end))
        .willReturn(List.of(new RoomRate(roomTypeId, end, 700_000L, "VND", ACTOR_ID, null, null)));
    var query = new GetRoomRatesQuery(ACTOR_ID, false, hotelId, roomTypeId, start, end);

    var result = service.execute(query);

    assertThat(result.rates()).extracting(r -> r.source()).containsExactly("NONE", "NONE", "RATE");
    assertThat(result.rates())
        .extracting(r -> r.effectivePrice())
        .containsExactly(null, null, 700_000L);

    givenRoomType(hotelId, roomTypeId, 500_000L);
    given(roomRateRepository.findRatesByRoomTypeIdAndDateRange(roomTypeId, start, end))
        .willReturn(List.of(new RoomRate(roomTypeId, start, 0L, "VND", ACTOR_ID, null, null)));
    var withInvalidOverride = service.execute(query);
    assertThat(withInvalidOverride.rates())
        .extracting(r -> r.source())
        .containsExactly("NONE", "BASE", "BASE");
    assertThat(withInvalidOverride.rates().get(0).effectivePrice()).isNull();
    assertThat(withInvalidOverride.rates().get(0).price()).isNull();
    then(roomRateRepository).should(never()).saveAll(any());
  }

  @Nested
  @DisplayName("GetRoomRatesUseCase.execute")
  class ExecuteTests {

    @Test
    @DisplayName("Ném lỗi khi ngày kết thúc trước ngày bắt đầu hoặc vượt quá 90 ngày")
    void shouldThrowWhenDatesInvalid() {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      LocalDate start = LocalDate.of(2026, 11, 10);
      LocalDate endBeforeStart = LocalDate.of(2026, 11, 5);
      LocalDate endTooFar = LocalDate.of(2027, 3, 15);

      // When & Then - Kết thúc trước bắt đầu
      PricingException dateOrderError =
          assertThrows(
              PricingException.class,
              () ->
                  service.execute(
                      new GetRoomRatesQuery(
                          ACTOR_ID, false, hotelId, roomTypeId, start, endBeforeStart)));
      assertEquals(PricingErrorCode.INVALID_DATE_RANGE, dateOrderError.code());

      // When & Then - Vượt quá 90 ngày
      PricingException maxRangeError =
          assertThrows(
              PricingException.class,
              () ->
                  service.execute(
                      new GetRoomRatesQuery(
                          ACTOR_ID, false, hotelId, roomTypeId, start, endTooFar)));
      assertEquals(PricingErrorCode.MAX_RANGE_EXCEEDED, maxRangeError.code());
    }

    @Test
    @DisplayName("Ném lỗi khi khách sạn hoặc loại phòng không tồn tại")
    void shouldThrowWhenHotelOrRoomTypeNotFound() {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      LocalDate start = LocalDate.of(2026, 11, 1);
      LocalDate end = LocalDate.of(2026, 11, 5);

      given(pricingHotelPort.findManagementAccess(hotelId, ACTOR_ID, false))
          .willReturn(Optional.empty());

      // When & Then - Không thấy khách sạn
      PricingException hotelError =
          assertThrows(
              PricingException.class,
              () ->
                  service.execute(
                      new GetRoomRatesQuery(ACTOR_ID, false, hotelId, roomTypeId, start, end)));
      assertEquals(PricingErrorCode.HOTEL_NOT_FOUND, hotelError.code());

      // Given - Khách sạn tồn tại nhưng loại phòng không thuộc khách sạn
      givenAccess(hotelId, Level.OWNER, HotelStatus.ACTIVE);
      given(pricingHotelPort.findActiveRoomType(roomTypeId, hotelId)).willReturn(Optional.empty());

      // When & Then - Không thấy loại phòng
      PricingException roomTypeError =
          assertThrows(
              PricingException.class,
              () ->
                  service.execute(
                      new GetRoomRatesQuery(ACTOR_ID, false, hotelId, roomTypeId, start, end)));
      assertEquals(PricingErrorCode.ROOM_TYPE_NOT_FOUND, roomTypeError.code());
    }

    @Test
    @DisplayName("Trả về danh sách giá chính xác từng ngày khi tra cứu thành công")
    void shouldReturnRatesSuccessfully() {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      LocalDate start = LocalDate.of(2026, 11, 1);
      LocalDate end = LocalDate.of(2026, 11, 2);
      Instant now = Instant.parse("2026-11-01T08:00:00Z");

      givenAccess(hotelId, Level.OWNER, HotelStatus.ACTIVE);
      givenRoomType(hotelId, roomTypeId, 500_000L);

      RoomRate rate =
          new RoomRate(roomTypeId, start, 1_500_000L, "VND", UUID.randomUUID(), now, now);

      given(roomRateRepository.findRatesByRoomTypeIdAndDateRange(roomTypeId, start, end))
          .willReturn(List.of(rate));

      // When
      RoomTypeRatesResult result =
          service.execute(new GetRoomRatesQuery(ACTOR_ID, false, hotelId, roomTypeId, start, end));

      // Then
      assertThat(result.hotelId()).isEqualTo(hotelId);
      assertThat(result.roomTypeId()).isEqualTo(roomTypeId);
      assertThat(result.currency()).isEqualTo("VND");
      assertThat(result.rates()).hasSize(2);

      var day1 = result.rates().get(0);
      assertThat(day1.date()).isEqualTo(start);
      assertThat(day1.hasRate()).isTrue();
      assertThat(day1.price()).isEqualTo(1_500_000L);

      var day2 = result.rates().get(1);
      assertThat(day2.date()).isEqualTo(end);
      assertThat(day2.hasRate()).isFalse();
      assertThat(day2.price()).isNull();
    }
  }

  @Nested
  @DisplayName("Phân quyền theo khách sạn")
  class AccessTests {

    @Test
    @DisplayName("Ném ACCESS_DENIED và không xét khách sạn khi người gọi là RECEPTIONIST")
    void shouldThrowAccessDeniedWhenHotelReportsDenied() {
      // Given
      givenAccess(hotelId, Level.RECEPTIONIST, HotelStatus.ACTIVE);

      // When & Then
      assertThatThrownBy(
              () ->
                  service.execute(
                      new GetRoomRatesQuery(
                          ACTOR_ID,
                          false,
                          hotelId,
                          roomTypeId,
                          LocalDate.of(2026, 11, 1),
                          LocalDate.of(2026, 11, 2))))
          .isInstanceOf(PricingException.class)
          .satisfies(
              ex ->
                  assertThat(((PricingException) ex).code())
                      .isEqualTo(PricingErrorCode.ACCESS_DENIED));
      then(pricingHotelPort).should(never()).findActiveRoomType(any(), any());
      then(roomRateRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName(
        "Ném HOTEL_NOT_FOUND khi người gọi không thuộc khách sạn hoặc khách sạn không tồn tại")
    void shouldThrowHotelNotFoundWhenHotelReportsNotFound() {
      // Given
      given(pricingHotelPort.findManagementAccess(hotelId, ACTOR_ID, false))
          .willReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(
              () ->
                  service.execute(
                      new GetRoomRatesQuery(
                          ACTOR_ID,
                          false,
                          hotelId,
                          roomTypeId,
                          LocalDate.of(2026, 11, 1),
                          LocalDate.of(2026, 11, 2))))
          .isInstanceOf(PricingException.class)
          .satisfies(
              ex ->
                  assertThat(((PricingException) ex).code())
                      .isEqualTo(PricingErrorCode.HOTEL_NOT_FOUND));
      then(pricingHotelPort).should(never()).findActiveRoomType(any(), any());
      then(roomRateRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Ném HOTEL_NOT_FOUND khi người gọi không có quan hệ nào với khách sạn")
    void shouldThrowHotelNotFoundWhenActorHasNoRelationToHotel() {
      // Given
      givenAccess(hotelId, Level.NONE, HotelStatus.ACTIVE);

      // When & Then
      assertThatThrownBy(
              () ->
                  service.execute(
                      new GetRoomRatesQuery(
                          ACTOR_ID,
                          false,
                          hotelId,
                          roomTypeId,
                          LocalDate.of(2026, 11, 1),
                          LocalDate.of(2026, 11, 2))))
          .satisfies(
              ex ->
                  assertThat(((PricingException) ex).code())
                      .isEqualTo(PricingErrorCode.HOTEL_NOT_FOUND));
      then(roomRateRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Ném HOTEL_LOCKED và không đụng giá khi khách sạn bị quản trị khóa, kể cả ADMIN")
    void shouldThrowHotelLockedWhenHotelIsLocked() {
      // Given
      givenAccess(hotelId, Level.ADMIN, HotelStatus.LOCKED);

      // When & Then
      assertThatThrownBy(
              () ->
                  service.execute(
                      new GetRoomRatesQuery(
                          ACTOR_ID,
                          false,
                          hotelId,
                          roomTypeId,
                          LocalDate.of(2026, 11, 1),
                          LocalDate.of(2026, 11, 2))))
          .satisfies(
              ex ->
                  assertThat(((PricingException) ex).code())
                      .isEqualTo(PricingErrorCode.HOTEL_LOCKED));
      then(pricingHotelPort).should(never()).findActiveRoomType(any(), any());
      then(roomRateRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Cho HOTEL_MANAGER thao tác giá khi khách sạn đang SUSPENDED (chưa/không mở bán)")
    void shouldAllowManagerWhenHotelIsSuspended() {
      // Given
      givenAccess(hotelId, Level.MANAGER, HotelStatus.SUSPENDED);
      givenRoomType(hotelId, roomTypeId, 500_000L);

      // When
      RoomTypeRatesResult result =
          service.execute(
              new GetRoomRatesQuery(
                  ACTOR_ID,
                  false,
                  hotelId,
                  roomTypeId,
                  LocalDate.of(2026, 11, 1),
                  LocalDate.of(2026, 11, 2)));

      // Then
      assertThat(result.hotelId()).isEqualTo(hotelId);
    }
  }
}
