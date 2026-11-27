package com.team.hotelbooking.inventory.infrastructure.integration;

import com.team.hotelbooking.booking.BookingRoomHoldGateway;
import com.team.hotelbooking.inventory.InventoryReservationDirectory;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BookingRoomHoldGatewayAdapter implements BookingRoomHoldGateway {

  private final InventoryReservationDirectory inventory;
  private final HotelCatalogPort hotels;
  private final com.team.hotelbooking.inventory.infrastructure.persistence.repository
          .RoomAssignmentJpaRepository
      assignments;
  private final java.time.Clock clock;

  @Override
  public void release(UUID bookingId, boolean expired) {
    release(bookingId, expired, null);
  }

  @Override
  public void release(UUID bookingId, boolean expired, UUID actorId) {
    inventory.release(
        bookingId,
        expired
            ? InventoryReservationDirectory.ReleaseReason.EXPIRED
            : InventoryReservationDirectory.ReleaseReason.CANCELLED);
    // All assignment/check-in mutations also lock the booking first.
    for (var assignment : assignments.findActiveByBookingId(bookingId)) {
      if (assignment.getStatus()
          != com.team.hotelbooking.inventory.infrastructure.persistence.entity
              .RoomAssignmentJpaEntity.DbRoomAssignmentStatus.ASSIGNED)
        throw new IllegalStateException("Cannot release an occupied room on cancellation");
      assignment.setStatus(
          com.team.hotelbooking.inventory.infrastructure.persistence.entity.RoomAssignmentJpaEntity
              .DbRoomAssignmentStatus.RELEASED);
      assignment.setReleasedAt(clock.instant());
      assignment.setReleasedByUserId(actorId);
      assignment.setReleaseReason(expired ? "Đơn hết hạn" : "Khách hủy đơn");
      assignments.saveAndFlush(assignment);
    }
  }

  @Override
  @Transactional
  public boolean confirmIfHeld(UUID bookingId, UUID hotelId, List<UUID> roomTypeIds) {
    // Cùng khóa với hold để không bán lại phòng trong lúc xác nhận sát hạn giữ chỗ.
    if (hotels.lockRoomTypesInHotel(hotelId, roomTypeIds).size() != roomTypeIds.size())
      return false;
    return inventory.confirmIfHeld(bookingId);
  }

  @Override
  public void hold(HoldRequest request) {
    inventory.hold(
        new InventoryReservationDirectory.HoldRequest(
            request.bookingId(),
            request.hotelId(),
            request.checkIn(),
            request.checkOut(),
            request.expiresAt(),
            request.items().stream()
                .map(
                    item ->
                        new InventoryReservationDirectory.HoldItemRequest(
                            item.bookingItemId(), item.roomTypeId(), item.quantity()))
                .toList()));
  }
}
