package com.team.hotelbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort.PhysicalRoomSummary;
import com.team.hotelbooking.inventory.infrastructure.persistence.adapter.JpaInventoryQueryAdapter;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.InventoryReservationJpaRepository;
import com.team.hotelbooking.inventory.infrastructure.persistence.repository.RoomBlockJpaRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Tính tồn của JpaInventoryQueryAdapter trên PostgreSQL thật (thay JpaInventoryQueryAdapterTests đã
 * bị xóa ở develop). HotelCatalogPort được mock để cố định phòng vật lý đang phục vụ; lịch chặn và
 * giữ phòng đọc từ DB qua đúng truy vấn của adapter.
 */
// Đặt ở package gốc vì TestcontainersConfiguration là package-private.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Tính tồn phòng của JpaInventoryQueryAdapter trên PostgreSQL (IV-03)")
class JpaInventoryQueryAdapterIntegrationTests {

  private static final UUID OWNER_ID = UUID.fromString("e1000000-0000-4000-8000-000000000001");
  private static final UUID HOTEL_ID = UUID.fromString("e1000000-0000-4000-8000-000000000101");
  private static final UUID ROOM_TYPE_ID = UUID.fromString("e1000000-0000-4000-8000-000000000201");
  private static final UUID EMPTY_ROOM_TYPE_ID =
      UUID.fromString("e1000000-0000-4000-8000-000000000202");
  private static final UUID ROOM_A = UUID.fromString("e1000000-0000-4000-8000-000000000301");
  private static final UUID ROOM_B = UUID.fromString("e1000000-0000-4000-8000-000000000302");
  private static final UUID ROOM_C = UUID.fromString("e1000000-0000-4000-8000-000000000303");
  private static final UUID DA_NANG_ID = UUID.fromString("822cafba-73f6-4ec0-bae7-71804535cae3");
  private static final LocalDate NIGHT_1 = LocalDate.of(2027, 6, 1);
  private static final LocalDate NIGHT_2 = LocalDate.of(2027, 6, 2);
  private static final LocalDate NIGHT_3 = LocalDate.of(2027, 6, 3);
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2027-05-01T00:00:00Z"), ZoneOffset.UTC);

  @Autowired private JdbcTemplate jdbc;
  @Autowired private RoomBlockJpaRepository roomBlockJpaRepository;
  @Autowired private InventoryReservationJpaRepository reservationJpaRepository;

  private HotelCatalogPort hotelCatalogPort;
  private JpaInventoryQueryAdapter adapter;

  @BeforeEach
  void setUp() {
    hotelCatalogPort = mock(HotelCatalogPort.class);
    adapter =
        new JpaInventoryQueryAdapter(
            hotelCatalogPort, roomBlockJpaRepository, reservationJpaRepository, CLOCK);

    jdbc.update(
        """
        INSERT INTO users (id, email, password_hash, full_name, phone, status, email_verified_at)
        VALUES (?, 'inventory-it-owner@test.local', 'not-a-real-hash', 'Owner tồn', '+84955000001',
                'active', now())
        ON CONFLICT (id) DO NOTHING
        """,
        OWNER_ID);
    jdbc.update(
        """
        INSERT INTO hotels (id, owner_user_id, city_id, name, slug, address, status)
        VALUES (?, ?, ?, 'Khách sạn tồn test', 'inventory-it-hotel', '1 Test', 'ACTIVE')
        ON CONFLICT (id) DO NOTHING
        """,
        HOTEL_ID,
        OWNER_ID,
        DA_NANG_ID);
    for (UUID roomTypeId : new UUID[] {ROOM_TYPE_ID, EMPTY_ROOM_TYPE_ID}) {
      jdbc.update(
          """
          INSERT INTO room_types (id, hotel_id, name, is_active) VALUES (?, ?, ?, true)
          ON CONFLICT (id) DO NOTHING
          """,
          roomTypeId,
          HOTEL_ID,
          "Loại " + roomTypeId.toString().substring(33));
    }
    int number = 1;
    for (UUID roomId : new UUID[] {ROOM_A, ROOM_B, ROOM_C}) {
      jdbc.update(
          """
          INSERT INTO rooms (id, hotel_id, room_type_id, room_number, operational_from)
          VALUES (?, ?, ?, ?, '2026-01-01T00:00:00Z')
          ON CONFLICT (id) DO NOTHING
          """,
          roomId,
          HOTEL_ID,
          ROOM_TYPE_ID,
          "IT" + number++);
    }

    jdbc.update("DELETE FROM room_blocks WHERE hotel_id = ?", HOTEL_ID);
    jdbc.update("DELETE FROM inventory_reservations WHERE room_type_id = ?", ROOM_TYPE_ID);
    // ROOM_A bị chặn bởi hai lịch chồng nhau (đếm một lần); một lịch chặn chưa gán phòng đêm 2.
    insertBlock(ROOM_A, NIGHT_1, NIGHT_3, "ACTIVE");
    insertBlock(ROOM_A, NIGHT_1, NIGHT_2, "ACTIVE");
    insertBlock(null, NIGHT_2, NIGHT_2, "ACTIVE");
    // Lịch đã RELEASED không trừ tồn.
    insertBlock(ROOM_B, NIGHT_1, NIGHT_3, "RELEASED");
    // Đêm 1: 1 giữ còn hạn; đêm 2: 1 đã xác nhận; đêm 3: 1 giữ đã hết hạn (không trừ).
    insertReservation(NIGHT_1, "HELD", Instant.parse("2027-05-02T00:00:00Z"));
    insertReservation(NIGHT_2, "CONFIRMED", null);
    insertReservation(NIGHT_3, "HELD", Instant.parse("2027-04-30T00:00:00Z"));
  }

  private void givenPhysicalRooms() {
    given(hotelCatalogPort.findPhysicalRoomsByRoomTypeId(ROOM_TYPE_ID))
        .willReturn(
            List.of(
                new PhysicalRoomSummary(ROOM_A, HOTEL_ID, ROOM_TYPE_ID, "ITA", null, null, null),
                new PhysicalRoomSummary(ROOM_B, HOTEL_ID, ROOM_TYPE_ID, "ITB", null, null, null),
                new PhysicalRoomSummary(ROOM_C, HOTEL_ID, ROOM_TYPE_ID, "ITC", null, null, null)));
    given(
            hotelCatalogPort.findPhysicalRoomsByRoomTypeIds(
                org.mockito.ArgumentMatchers.anyCollection()))
        .willReturn(
            List.of(
                new PhysicalRoomSummary(ROOM_A, HOTEL_ID, ROOM_TYPE_ID, "ITA", null, null, null),
                new PhysicalRoomSummary(ROOM_B, HOTEL_ID, ROOM_TYPE_ID, "ITB", null, null, null),
                new PhysicalRoomSummary(ROOM_C, HOTEL_ID, ROOM_TYPE_ID, "ITC", null, null, null)));
  }

  private void insertBlock(UUID roomId, LocalDate start, LocalDate end, String status) {
    jdbc.update(
        """
        INSERT INTO room_blocks
          (hotel_id, room_type_id, room_id, start_date, end_date, status, created_by_user_id, released_at)
        VALUES (?, ?, ?, ?, ?, CAST(? AS room_block_status__t), ?, ?)
        """,
        HOTEL_ID,
        ROOM_TYPE_ID,
        roomId,
        start,
        end,
        status,
        OWNER_ID,
        "RELEASED".equals(status) ? Timestamp.from(CLOCK.instant()) : null);
  }

  private void insertReservation(LocalDate night, String status, Instant expiresAt) {
    jdbc.update(
        """
        INSERT INTO inventory_reservations (room_type_id, stay_date, quantity, status, expires_at)
        VALUES (?, ?, 1, CAST(? AS inventory_reservation_status__t), ?)
        """,
        ROOM_TYPE_ID,
        night,
        status,
        expiresAt == null ? null : Timestamp.from(expiresAt));
  }

  @Nested
  @DisplayName("calculateAvailableQuantity (một loại phòng)")
  class SingleTests {

    @Test
    @DisplayName("Trả 0 khi loại phòng không có phòng đang phục vụ")
    void shouldReturnZeroWhenNoOperationalRooms() {
      given(hotelCatalogPort.countActiveRoomsByRoomTypeId(EMPTY_ROOM_TYPE_ID)).willReturn(0L);

      assertThat(adapter.calculateAvailableQuantity(EMPTY_ROOM_TYPE_ID, NIGHT_1, NIGHT_3)).isZero();
    }

    @Test
    @DisplayName("Đêm 1 còn 1: trừ ROOM_A một lần (hai lịch chồng) và 1 giữ còn hạn")
    void shouldCountOverlappingBlocksOfSameRoomOnceWhenOnlyFirstNight() {
      givenPhysicalRooms();

      assertThat(adapter.calculateAvailableQuantity(ROOM_TYPE_ID, NIGHT_1, NIGHT_2)).isEqualTo(1);
    }

    @Test
    @DisplayName("Lấy đêm thấp nhất: đêm 2 hết phòng (ROOM_A, chặn chưa gán, 1 đã xác nhận)")
    void shouldReturnMinimumAcrossNightsWhenSecondNightIsFull() {
      givenPhysicalRooms();

      assertThat(adapter.calculateAvailableQuantity(ROOM_TYPE_ID, NIGHT_1, NIGHT_3)).isZero();
    }

    @Test
    @DisplayName("Đêm 3 còn 2: giữ phòng hết hạn và lịch RELEASED không trừ tồn")
    void shouldIgnoreExpiredHoldAndReleasedBlockWhenThirdNight() {
      givenPhysicalRooms();

      assertThat(adapter.calculateAvailableQuantity(ROOM_TYPE_ID, NIGHT_3, NIGHT_3.plusDays(1)))
          .isEqualTo(2);
    }
  }

  @Nested
  @DisplayName("calculateMinAvailableQuantities (theo lô)")
  class BatchTests {

    @Test
    @DisplayName("Cho cùng kết quả với hàm đơn và trả 0 cho loại phòng không có phòng")
    void shouldMatchSingleCalculationWhenBatched() {
      givenPhysicalRooms();
      // When
      Map<UUID, Integer> oneNight =
          adapter.calculateMinAvailableQuantities(
              Map.of(ROOM_TYPE_ID, 3L, EMPTY_ROOM_TYPE_ID, 0L), NIGHT_1, NIGHT_2);
      Map<UUID, Integer> twoNights =
          adapter.calculateMinAvailableQuantities(Map.of(ROOM_TYPE_ID, 3L), NIGHT_1, NIGHT_3);

      // Then
      assertThat(oneNight).containsEntry(ROOM_TYPE_ID, 1).containsEntry(EMPTY_ROOM_TYPE_ID, 0);
      assertThat(twoNights).containsEntry(ROOM_TYPE_ID, 0);
    }
  }
}
