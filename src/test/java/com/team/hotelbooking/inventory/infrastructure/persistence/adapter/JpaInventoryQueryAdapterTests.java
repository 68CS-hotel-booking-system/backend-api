package com.team.hotelbooking.inventory.infrastructure.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.InventoryReservationJpaEntity;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.InventoryReservationJpaEntity.DbReservationStatus;
import com.team.hotelbooking.inventory.infrastructure.persistence.entity.RoomBlockJpaEntity;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.InventoryReservationJpaRepository;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.RoomBlockJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Adapter truy vấn khả dụng phòng vật lý (JpaInventoryQueryAdapter)")
class JpaInventoryQueryAdapterTests {

  @Mock private HotelCatalogPort hotelCatalogPort;
  @Mock private RoomBlockJpaRepository roomBlockJpaRepository;
  @Mock private InventoryReservationJpaRepository reservationJpaRepository;

  private Clock clock;
  private JpaInventoryQueryAdapter adapter;

  @BeforeEach
  void setUp() {
    clock = Clock.fixed(Instant.parse("2026-11-10T08:00:00Z"), ZoneOffset.UTC);
    adapter =
        new JpaInventoryQueryAdapter(
            hotelCatalogPort, roomBlockJpaRepository, reservationJpaRepository, clock);
  }

  @Test
  void shouldReturnZeroInBatchWhenNoRoomIsFreeForWholeStay() {
    UUID type = UUID.randomUUID(), hotel = UUID.randomUUID();
    UUID a = UUID.randomUUID(), b = UUID.randomUUID();
    LocalDate first = LocalDate.of(2027, 1, 1), second = first.plusDays(1);
    var roomA = new HotelCatalogPort.PhysicalRoomSummary(a, hotel, type, "A", null, null, null);
    var roomB = new HotelCatalogPort.PhysicalRoomSummary(b, hotel, type, "B", null, null, null);
    RoomBlockJpaEntity blockA = new RoomBlockJpaEntity();
    blockA.setRoomTypeId(type);
    blockA.setRoomId(a);
    blockA.setStartDate(first);
    blockA.setEndDate(first);
    RoomBlockJpaEntity blockB = new RoomBlockJpaEntity();
    blockB.setRoomTypeId(type);
    blockB.setRoomId(b);
    blockB.setStartDate(second);
    blockB.setEndDate(second);
    given(
            hotelCatalogPort.findPhysicalRoomsByRoomTypeIds(
                org.mockito.ArgumentMatchers.anyCollection()))
        .willReturn(List.of(roomA, roomB));
    given(
            roomBlockJpaRepository.findActiveBlocksInRangeForRoomTypes(
                org.mockito.ArgumentMatchers.anyCollection(), any(), any()))
        .willReturn(List.of(blockA, blockB));
    var result =
        adapter.calculateMinAvailableQuantities(
            java.util.Map.of(type, 2L), first, first.plusDays(2));
    assertEquals(0, result.get(type));
  }

  @Nested
  @DisplayName("JpaInventoryQueryAdapter.calculateAvailableQuantity")
  class CalculateAvailableQuantityTests {

    @Test
    @DisplayName(
        "Trả về 0 khi hai phòng vật lý bị chặn chéo đêm không tạo thành phòng liên tục cả kỳ (IV-13)")
    void shouldReturnZeroWhenBlocksAlternateAcrossTwoRooms() {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      UUID room301Id = UUID.randomUUID();
      UUID room302Id = UUID.randomUUID();
      LocalDate d10 = LocalDate.of(2026, 11, 10);
      LocalDate d11 = LocalDate.of(2026, 11, 11);
      LocalDate d12 = LocalDate.of(2026, 11, 12);

      given(hotelCatalogPort.findPhysicalRoomsByRoomTypeId(roomTypeId))
          .willReturn(
              List.of(
                  new HotelCatalogPort.PhysicalRoomSummary(
                      room301Id, hotelId, roomTypeId, "301", "3", null, null),
                  new HotelCatalogPort.PhysicalRoomSummary(
                      room302Id, hotelId, roomTypeId, "302", "3", null, null)));

      RoomBlockJpaEntity block301 = new RoomBlockJpaEntity();
      block301.setRoomTypeId(roomTypeId);
      block301.setRoomId(room301Id);
      block301.setStartDate(d10);
      block301.setEndDate(d10);

      RoomBlockJpaEntity block302 = new RoomBlockJpaEntity();
      block302.setRoomTypeId(roomTypeId);
      block302.setRoomId(room302Id);
      block302.setStartDate(d11);
      block302.setEndDate(d11);

      given(roomBlockJpaRepository.findActiveBlocksInRange(eq(roomTypeId), any(), any()))
          .willReturn(List.of(block301, block302));
      given(
              reservationJpaRepository.findActiveReservationsInRange(
                  eq(roomTypeId), any(), any(), eq(clock.instant())))
          .willReturn(List.of());

      // When
      int available = adapter.calculateAvailableQuantity(roomTypeId, d10, d12);

      // Then
      assertEquals(0, available);
    }

    @Test
    @DisplayName("Gom đúng các đêm của cùng bookingItemId để tính số phòng còn lại chính xác")
    void shouldGroupReservationsByBookingItemAndCalculateRemainingRooms() {
      // Given
      UUID hotelId = UUID.randomUUID();
      UUID roomTypeId = UUID.randomUUID();
      UUID room301Id = UUID.randomUUID();
      UUID room302Id = UUID.randomUUID();
      UUID bookingItemId = UUID.randomUUID();
      LocalDate d10 = LocalDate.of(2026, 11, 10);
      LocalDate d11 = LocalDate.of(2026, 11, 11);
      LocalDate d12 = LocalDate.of(2026, 11, 12);

      given(hotelCatalogPort.findPhysicalRoomsByRoomTypeId(roomTypeId))
          .willReturn(
              List.of(
                  new HotelCatalogPort.PhysicalRoomSummary(
                      room301Id, hotelId, roomTypeId, "301", "3", null, null),
                  new HotelCatalogPort.PhysicalRoomSummary(
                      room302Id, hotelId, roomTypeId, "302", "3", null, null)));

      given(roomBlockJpaRepository.findActiveBlocksInRange(eq(roomTypeId), any(), any()))
          .willReturn(List.of());

      InventoryReservationJpaEntity resNight1 = new InventoryReservationJpaEntity();
      resNight1.setBookingItemId(bookingItemId);
      resNight1.setRoomTypeId(roomTypeId);
      resNight1.setStayDate(d10);
      resNight1.setQuantity((short) 1);
      resNight1.setStatus(DbReservationStatus.CONFIRMED);

      InventoryReservationJpaEntity resNight2 = new InventoryReservationJpaEntity();
      resNight2.setBookingItemId(bookingItemId);
      resNight2.setRoomTypeId(roomTypeId);
      resNight2.setStayDate(d11);
      resNight2.setQuantity((short) 1);
      resNight2.setStatus(DbReservationStatus.CONFIRMED);

      given(
              reservationJpaRepository.findActiveReservationsInRange(
                  eq(roomTypeId), any(), any(), eq(clock.instant())))
          .willReturn(List.of(resNight1, resNight2));

      // When
      int available = adapter.calculateAvailableQuantity(roomTypeId, d10, d12);

      // Then
      assertEquals(1, available);
    }
  }
}
