package com.team.hotelbooking.inventory.infrastructure.integration;

import com.team.hotelbooking.booking.BookingRoomHoldGateway;
import com.team.hotelbooking.inventory.InventoryReservationDirectory;
import com.team.hotelbooking.inventory.InventoryReservationDirectory.ReleaseReason;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingRoomHoldGatewayAdapter implements BookingRoomHoldGateway {

  private final InventoryReservationDirectory inventory;

  @Override
  public void release(UUID bookingId, boolean expired) {
    inventory.release(bookingId, expired ? ReleaseReason.EXPIRED : ReleaseReason.CANCELLED);
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
