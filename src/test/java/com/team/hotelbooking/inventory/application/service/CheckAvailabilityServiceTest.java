package com.team.hotelbooking.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.team.hotelbooking.hotel.infrastructure.persistence.entity.HotelJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.entity.RoomTypeJpaEntity;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelJpaRepository;
import com.team.hotelbooking.hotel.infrastructure.persistence.repository.RoomTypeJpaRepository;
import com.team.hotelbooking.inventory.application.command.CheckAvailabilityQuery;
import com.team.hotelbooking.inventory.application.port.InventoryQueryPort;
import com.team.hotelbooking.inventory.application.port.RoomPricingPort;
import com.team.hotelbooking.inventory.domain.exception.InventoryException;
import com.team.hotelbooking.inventory.domain.model.HotelAvailability;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CheckAvailabilityServiceTest {

  private HotelJpaRepository hotelJpaRepository;
  private RoomTypeJpaRepository roomTypeJpaRepository;
  private InventoryQueryPort inventoryQueryPort;
  private RoomPricingPort roomPricingPort;
  private CheckAvailabilityService service;

  @BeforeEach
  void setUp() {
    hotelJpaRepository = mock(HotelJpaRepository.class);
    roomTypeJpaRepository = mock(RoomTypeJpaRepository.class);
    inventoryQueryPort = mock(InventoryQueryPort.class);
    roomPricingPort = mock(RoomPricingPort.class);
    service =
        new CheckAvailabilityService(
            hotelJpaRepository, roomTypeJpaRepository, inventoryQueryPort, roomPricingPort);
  }

  @Test
  @DisplayName("Ném lỗi khi check-in trong quá khứ hoặc check-out trước check-in")
  void shouldThrowWhenDatesInvalid() {
    UUID hotelId = UUID.randomUUID();

    // Check-in quá khứ
    assertThrows(
        InventoryException.class,
        () ->
            service.execute(
                new CheckAvailabilityQuery(
                    hotelId, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), 2, 0, 1)));

    // Check-out trước check-in
    assertThrows(
        InventoryException.class,
        () ->
            service.execute(
                new CheckAvailabilityQuery(
                    hotelId, LocalDate.now().plusDays(5), LocalDate.now().plusDays(4), 2, 0, 1)));
  }

  @Test
  @DisplayName("Tính toán chính xác giá phòng kết hợp giá riêng và basePrice khi có phòng trống")
  void shouldCalculateAvailabilityAndPriceCorrectly() {
    UUID hotelId = UUID.randomUUID();
    UUID roomTypeId = UUID.randomUUID();

    HotelJpaEntity hotel = new HotelJpaEntity();
    hotel.setId(hotelId);
    hotel.setStatus(HotelJpaEntity.DbHotelStatus.active);
    when(hotelJpaRepository.findByIdAndActiveTrue(hotelId)).thenReturn(Optional.of(hotel));

    RoomTypeJpaEntity rt = new RoomTypeJpaEntity();
    rt.setId(roomTypeId);
    rt.setHotelId(hotelId);
    rt.setName("Deluxe Ocean View");
    rt.setMaxOccupancy((short) 4);
    rt.setMaxAdults((short) 2);
    rt.setMaxChildren((short) 2);
    rt.setBasePrice(1_000_000L);
    when(roomTypeJpaRepository.findAllByHotelIdAndActiveTrue(hotelId)).thenReturn(List.of(rt));

    LocalDate checkIn = LocalDate.now().plusDays(10);
    LocalDate checkOut = LocalDate.now().plusDays(12); // 2 đêm: day10, day11

    // Inventory có 2 phòng trống
    when(inventoryQueryPort.calculateAvailableQuantity(roomTypeId, checkIn, checkOut))
        .thenReturn(2);

    // Ngày 1 có custom rate 1.200.000, ngày 2 không có -> dùng basePrice 1.000.000
    when(roomPricingPort.getDailyRates(roomTypeId, checkIn, checkOut.minusDays(1)))
        .thenReturn(Map.of(checkIn, 1_200_000L));

    HotelAvailability result =
        service.execute(new CheckAvailabilityQuery(hotelId, checkIn, checkOut, 2, 0, 1));

    assertThat(result.hotelId()).isEqualTo(hotelId);
    assertThat(result.totalNights()).isEqualTo(2);
    assertThat(result.roomTypes()).hasSize(1);

    var roomTypeRes = result.roomTypes().get(0);
    assertThat(roomTypeRes.availableQuantity()).isEqualTo(2);
    assertThat(roomTypeRes.isAvailable()).isTrue();
    assertThat(roomTypeRes.totalEstimatedPrice()).isEqualTo(2_200_000L); // 1.2M + 1.0M
    assertThat(roomTypeRes.pricePerNight()).isEqualTo(1_100_000L);
  }
}
