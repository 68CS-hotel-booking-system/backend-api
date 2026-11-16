package com.team.hotelbooking.booking.application.usecase;

import com.team.hotelbooking.booking.application.result.BookingDetailResult;
import java.util.UUID;

public interface CancelBookingUseCase {
  BookingDetailResult execute(UUID customerId, UUID bookingId);
}
