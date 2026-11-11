package com.team.hotelbooking.inventory.infrastructure.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team.hotelbooking.inventory.InventoryReservationDirectory.HoldItemRequest;
import com.team.hotelbooking.inventory.InventoryReservationDirectory.HoldOutcome;
import com.team.hotelbooking.inventory.InventoryReservationDirectory.HoldRequest;
import com.team.hotelbooking.inventory.InventoryReservationDirectory.ReleaseOutcome;
import com.team.hotelbooking.inventory.InventoryReservationDirectory.ReleaseReason;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.port.InventoryQueryPort;
import com.team.hotelbooking.inventory.application.port.RoomPricingPort;
import com.team.hotelbooking.inventory.domain.exception.InventoryErrorCode;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.InventoryReservationJpaEntity;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.InventoryReservationJpaEntity.DbReservationStatus;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.InventoryReservationJpaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("Contract giữ, xác nhận và trả tồn kho (InventoryReservationDirectoryAdapter)")
class InventoryReservationDirectoryAdapterTests {

  @Mock private HotelCatalogPort hotelCatalogPort;
  @Mock private InventoryQueryPort inventoryQueryPort;
  @Mock private RoomPricingPort roomPricingPort;
  @Mock private InventoryReservationJpaRepository reservationJpaRepository;
  @Mock private PlatformTransactionManager transactionManager;

  private Clock clock;
  private Instant now;
  private InventoryReservationDirectoryAdapter adapter;

  @BeforeEach
  void setUp() {
    now = Instant.parse("2026-11-10T10:00:00Z");
    clock = Clock.fixed(now, ZoneOffset.UTC);
    given(transactionManager.getTransaction(any())).willReturn(new SimpleTransactionStatus());
    adapter =
        new InventoryReservationDirectoryAdapter(
            hotelCatalogPort,
            inventoryQueryPort,
            roomPricingPort,
            reservationJpaRepository,
            clock,
            transactionManager);
  }

  @Nested
  @DisplayName("InventoryReservationDirectory.hold")
  class HoldTests {

    @Test
    @DisplayName("Giữ tồn thành công khi đủ phòng liên tục và mọi đêm đều có giá hợp lệ")
    void shouldHoldInventoryWhenAvailableAndRatesAreValid() {
      // Given
      UUID bookingId = UUID.randomUUID();
      UUID bookingItemId = UUID.randomUUID();
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      LocalDate checkIn = LocalDate.of(2026, 11, 12);
      LocalDate checkOut = LocalDate.of(2026, 11, 14);
      Instant expiresAt = now.plus(Duration.ofMinutes(15));

      given(hotelCatalogPort.findHotelSummary(hotelId))
          .willReturn(Optional.of(new HotelCatalogPort.HotelSummary(hotelId, true, false)));
      given(hotelCatalogPort.lockActiveRoomTypesInHotel(hotelId, Set.of(roomTypeId)))
          .willReturn(
              List.of(
                  new HotelCatalogPort.RoomTypeSummary(roomTypeId, "Deluxe", 2, 2, 0, 1_000_000L)));
      given(reservationJpaRepository.lockAllByBookingIdOrderByStayDateAscIdAsc(bookingId))
          .willReturn(List.of());
      given(roomPricingPort.getDailyRates(roomTypeId, checkIn, checkOut.minusDays(1)))
          .willReturn(Map.of(checkIn, 1_200_000L));
      given(inventoryQueryPort.calculateAvailableQuantity(roomTypeId, checkIn, checkOut))
          .willReturn(2);

      HoldRequest request =
          new HoldRequest(
              bookingId,
              hotelId,
              checkIn,
              checkOut,
              expiresAt,
              List.of(new HoldItemRequest(bookingItemId, roomTypeId, 2)));

      // When
      HoldOutcome outcome = adapter.hold(request);

      // Then
      assertEquals(bookingId, outcome.bookingId());
      assertEquals(expiresAt, outcome.expiresAt());
      assertEquals(2, outcome.totalReservedRows());
      assertFalse(outcome.replayed());
      verify(reservationJpaRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Trả về kết quả đã giữ trước đó khi gọi lặp cùng bookingId (BK-12)")
    void shouldReturnReplayedOutcomeWhenHoldCalledRepeatedlyWithSameBookingId() {
      // Given
      UUID bookingId = UUID.randomUUID();
      UUID bookingItemId = UUID.randomUUID();
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      LocalDate checkIn = LocalDate.of(2026, 11, 12);
      LocalDate checkOut = LocalDate.of(2026, 11, 14);
      Instant originalExpiresAt = now.plus(Duration.ofMinutes(15));

      given(hotelCatalogPort.findHotelSummary(hotelId))
          .willReturn(Optional.of(new HotelCatalogPort.HotelSummary(hotelId, true, false)));
      given(hotelCatalogPort.lockActiveRoomTypesInHotel(hotelId, Set.of(roomTypeId)))
          .willReturn(
              List.of(
                  new HotelCatalogPort.RoomTypeSummary(roomTypeId, "Deluxe", 2, 2, 0, 1_000_000L)));

      InventoryReservationJpaEntity existingRow = new InventoryReservationJpaEntity();
      existingRow.setBookingId(bookingId);
      existingRow.setBookingItemId(bookingItemId);
      existingRow.setRoomTypeId(roomTypeId);
      existingRow.setStayDate(checkIn);
      existingRow.setQuantity((short) 1);
      existingRow.setStatus(DbReservationStatus.HELD);
      existingRow.setExpiresAt(originalExpiresAt);

      given(reservationJpaRepository.lockAllByBookingIdOrderByStayDateAscIdAsc(bookingId))
          .willReturn(List.of(existingRow));

      HoldRequest request =
          new HoldRequest(
              bookingId,
              hotelId,
              checkIn,
              checkOut,
              now.plus(Duration.ofMinutes(20)),
              List.of(new HoldItemRequest(bookingItemId, roomTypeId, 1)));

      // When
      HoldOutcome outcome = adapter.hold(request);

      // Then
      assertTrue(outcome.replayed());
      assertEquals(originalExpiresAt, outcome.expiresAt());
      assertEquals(1, outcome.totalReservedRows());
      verify(reservationJpaRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Ném lỗi INSUFFICIENT_INVENTORY khi số phòng trống liên tục không đủ (IV-08)")
    void shouldThrowInsufficientInventoryWhenAvailableLessThanRequested() {
      // Given
      UUID bookingId = UUID.randomUUID();
      UUID bookingItemId = UUID.randomUUID();
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      LocalDate checkIn = LocalDate.of(2026, 11, 12);
      LocalDate checkOut = LocalDate.of(2026, 11, 14);

      given(hotelCatalogPort.findHotelSummary(hotelId))
          .willReturn(Optional.of(new HotelCatalogPort.HotelSummary(hotelId, true, false)));
      given(hotelCatalogPort.lockActiveRoomTypesInHotel(hotelId, Set.of(roomTypeId)))
          .willReturn(
              List.of(
                  new HotelCatalogPort.RoomTypeSummary(roomTypeId, "Deluxe", 2, 2, 0, 1_000_000L)));
      given(reservationJpaRepository.lockAllByBookingIdOrderByStayDateAscIdAsc(bookingId))
          .willReturn(List.of());
      given(roomPricingPort.getDailyRates(roomTypeId, checkIn, checkOut.minusDays(1)))
          .willReturn(Map.of());
      given(inventoryQueryPort.calculateAvailableQuantity(roomTypeId, checkIn, checkOut))
          .willReturn(1);

      HoldRequest request =
          new HoldRequest(
              bookingId,
              hotelId,
              checkIn,
              checkOut,
              now.plus(Duration.ofMinutes(15)),
              List.of(new HoldItemRequest(bookingItemId, roomTypeId, 2)));

      // When
      InventoryException error =
          assertThrows(InventoryException.class, () -> adapter.hold(request));

      // Then
      assertEquals(InventoryErrorCode.INSUFFICIENT_INVENTORY, error.code());
      verify(reservationJpaRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Ném lỗi RATE_NOT_SET khi có đêm chưa được thiết lập giá hợp lệ (PR-02)")
    void shouldThrowRateNotSetWhenAnyNightHasZeroOrMissingPrice() {
      // Given
      UUID bookingId = UUID.randomUUID();
      UUID bookingItemId = UUID.randomUUID();
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      LocalDate checkIn = LocalDate.of(2026, 11, 12);
      LocalDate checkOut = LocalDate.of(2026, 11, 14);

      given(hotelCatalogPort.findHotelSummary(hotelId))
          .willReturn(Optional.of(new HotelCatalogPort.HotelSummary(hotelId, true, false)));
      given(hotelCatalogPort.lockActiveRoomTypesInHotel(hotelId, Set.of(roomTypeId)))
          .willReturn(
              List.of(new HotelCatalogPort.RoomTypeSummary(roomTypeId, "Deluxe", 2, 2, 0, 0L)));
      given(reservationJpaRepository.lockAllByBookingIdOrderByStayDateAscIdAsc(bookingId))
          .willReturn(List.of());
      given(roomPricingPort.getDailyRates(roomTypeId, checkIn, checkOut.minusDays(1)))
          .willReturn(Map.of(checkIn, 1_000_000L));

      HoldRequest request =
          new HoldRequest(
              bookingId,
              hotelId,
              checkIn,
              checkOut,
              now.plus(Duration.ofMinutes(15)),
              List.of(new HoldItemRequest(bookingItemId, roomTypeId, 1)));

      // When
      InventoryException error =
          assertThrows(InventoryException.class, () -> adapter.hold(request));

      // Then
      assertEquals(InventoryErrorCode.RATE_NOT_SET, error.code());
    }
  }

  @Nested
  @DisplayName("InventoryReservationDirectory.confirm")
  class ConfirmTests {

    @Test
    @DisplayName("Chuyển HELD sang CONFIRMED khi giữ chỗ còn hạn")
    void shouldTransitionHeldToConfirmedWhenNotExpired() {
      // Given
      UUID bookingId = UUID.randomUUID();
      InventoryReservationJpaEntity heldRow = new InventoryReservationJpaEntity();
      heldRow.setBookingId(bookingId);
      heldRow.setStatus(DbReservationStatus.HELD);
      heldRow.setExpiresAt(now.plusSeconds(60));

      given(reservationJpaRepository.lockAllByBookingIdOrderByStayDateAscIdAsc(bookingId))
          .willReturn(List.of(heldRow));

      // When
      adapter.confirm(bookingId);

      // Then
      assertEquals(DbReservationStatus.CONFIRMED, heldRow.getStatus());
      assertEquals(now, heldRow.getConfirmedAt());
      verify(reservationJpaRepository).saveAll(List.of(heldRow));
    }

    @Test
    @DisplayName("Ném lỗi HOLD_EXPIRED khi xác nhận tại hoặc sau thời điểm hết hạn giữ chỗ (IV-06)")
    void shouldThrowHoldExpiredWhenConfirmingAtExactExpirationTime() {
      // Given
      UUID bookingId = UUID.randomUUID();
      InventoryReservationJpaEntity expiredRow = new InventoryReservationJpaEntity();
      expiredRow.setBookingId(bookingId);
      expiredRow.setStatus(DbReservationStatus.HELD);
      expiredRow.setExpiresAt(now); // Tại đúng thời điểm hết hạn

      given(reservationJpaRepository.lockAllByBookingIdOrderByStayDateAscIdAsc(bookingId))
          .willReturn(List.of(expiredRow));

      // When
      InventoryException error =
          assertThrows(InventoryException.class, () -> adapter.confirm(bookingId));

      // Then
      assertEquals(InventoryErrorCode.HOLD_EXPIRED, error.code());
      verify(reservationJpaRepository, never()).saveAll(anyList());
    }
  }

  @Nested
  @DisplayName("InventoryReservationDirectory.release & releaseUnusedNights")
  class ReleaseTests {

    @Test
    @DisplayName("Giải phóng tồn kho đúng một lần; gọi lặp trả về releasedRowCount = 0 (IV-07)")
    void shouldReleaseOnceAndReturnZeroOnDuplicateReleaseCall() {
      // Given
      UUID bookingId = UUID.randomUUID();
      InventoryReservationJpaEntity row = new InventoryReservationJpaEntity();
      row.setBookingId(bookingId);
      row.setStayDate(LocalDate.of(2026, 11, 12));
      row.setStatus(DbReservationStatus.HELD);
      row.setExpiresAt(now.plusSeconds(300));

      given(reservationJpaRepository.lockAllByBookingIdOrderByStayDateAscIdAsc(bookingId))
          .willReturn(List.of(row));

      // When
      ReleaseOutcome first = adapter.release(bookingId, ReleaseReason.EXPIRED);
      ReleaseOutcome second = adapter.release(bookingId, ReleaseReason.EXPIRED);

      // Then
      assertEquals(1, first.releasedRowCount());
      assertEquals(DbReservationStatus.RELEASED, row.getStatus());
      assertEquals("EXPIRED", row.getReleaseReason());
      assertEquals(0, second.releasedRowCount());
    }

    @Test
    @DisplayName(
        "Chỉ giải phóng các đêm chưa sử dụng từ fromDate trở đi khi NO_SHOW, giữ nguyên đêm đã qua (IV-11, BK-14)")
    void shouldOnlyReleaseUnusedFutureNightsOnNoShow() {
      // Given
      UUID bookingId = UUID.randomUUID();
      LocalDate d10 = LocalDate.of(2026, 11, 10);
      LocalDate d11 = LocalDate.of(2026, 11, 11);
      LocalDate d12 = LocalDate.of(2026, 11, 12);

      InventoryReservationJpaEntity night10 = new InventoryReservationJpaEntity();
      night10.setBookingId(bookingId);
      night10.setStayDate(d10);
      night10.setStatus(DbReservationStatus.CONFIRMED);

      InventoryReservationJpaEntity night11 = new InventoryReservationJpaEntity();
      night11.setBookingId(bookingId);
      night11.setStayDate(d11);
      night11.setStatus(DbReservationStatus.CONFIRMED);

      InventoryReservationJpaEntity night12 = new InventoryReservationJpaEntity();
      night12.setBookingId(bookingId);
      night12.setStayDate(d12);
      night12.setStatus(DbReservationStatus.CONFIRMED);

      given(reservationJpaRepository.lockAllByBookingIdOrderByStayDateAscIdAsc(bookingId))
          .willReturn(List.of(night10, night11, night12));

      // When: đánh dấu NO_SHOW vào ngày 11/11 -> trả tồn đêm 11/11 và 12/11, giữ nguyên đêm 10/11
      ReleaseOutcome first = adapter.releaseUnusedNights(bookingId, d11);
      ReleaseOutcome second = adapter.releaseUnusedNights(bookingId, d11);

      // Then
      assertEquals(2, first.releasedRowCount());
      assertEquals(0, second.releasedRowCount());
      assertEquals(DbReservationStatus.CONFIRMED, night10.getStatus());
      assertEquals(DbReservationStatus.RELEASED, night11.getStatus());
      assertEquals(DbReservationStatus.RELEASED, night12.getStatus());
      assertEquals("NO_SHOW", night11.getReleaseReason());
    }
  }
}
