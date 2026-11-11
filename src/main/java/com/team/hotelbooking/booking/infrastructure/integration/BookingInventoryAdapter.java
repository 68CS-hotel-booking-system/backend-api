package com.team.hotelbooking.booking.infrastructure.integration;

import com.team.hotelbooking.booking.BookingRoomHoldGateway;
import com.team.hotelbooking.booking.BookingRoomHoldGateway.HoldItem;
import com.team.hotelbooking.booking.BookingRoomHoldGateway.HoldRequest;
import com.team.hotelbooking.booking.application.port.BookingInventoryPort;
import com.team.hotelbooking.booking.domain.model.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingInventoryAdapter implements BookingInventoryPort {

  private final BookingRoomHoldGateway inventory;

  @Override
  public void hold(Booking booking) {
    var items =
        booking.items().stream()
            .map(item -> new HoldItem(item.id(), item.roomTypeId(), item.quantity()))
            .toList();

    inventory.hold(
        new HoldRequest(
            booking.id(),
            booking.hotelId(),
            booking.checkIn(),
            booking.checkOut(),
            booking.expiresAt(),
            items));
  }
}
