package com.team.hotelbooking.booking.application.usecase;

import com.team.hotelbooking.booking.application.port.BookingPricingPort.StayQuoteCriteria;
import com.team.hotelbooking.booking.application.result.VoucherQuoteResult;
import java.util.UUID;

public interface QuoteVoucherUseCase {
  VoucherQuoteResult execute(UUID customerId, String voucherCode, StayQuoteCriteria stay);
}
