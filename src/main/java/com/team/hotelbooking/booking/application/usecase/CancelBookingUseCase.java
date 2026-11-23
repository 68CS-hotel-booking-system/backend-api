package com.team.hotelbooking.booking.application.usecase;

import com.team.hotelbooking.booking.application.result.CancelBookingResult;
import java.util.UUID;

public interface CancelBookingUseCase {
  CancelBookingResult execute(UUID customerId, UUID bookingId);
}
