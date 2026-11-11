package com.team.hotelbooking.inventory.infrastructure.integration;

import com.team.hotelbooking.booking.BookingRoomHoldGateway;
import com.team.hotelbooking.inventory.InventoryReservationDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingRoomHoldGatewayAdapter implements BookingRoomHoldGateway {

  private final InventoryReservationDirectory inventory;

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
