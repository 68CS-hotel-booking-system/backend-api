package com.team.hotelbooking.booking.application.usecase.impl;

import com.team.hotelbooking.booking.application.port.BookingIdentityPort;
import com.team.hotelbooking.booking.application.port.BookingPricingPort;
import com.team.hotelbooking.booking.application.port.BookingPricingPort.StayQuoteCriteria;
import com.team.hotelbooking.booking.application.result.VoucherQuoteResult;
import com.team.hotelbooking.booking.application.service.VoucherApplicationService;
import com.team.hotelbooking.booking.application.usecase.QuoteVoucherUseCase;
import com.team.hotelbooking.booking.domain.exception.BookingErrorCode;
import com.team.hotelbooking.booking.domain.exception.BookingException;
import java.util.UUID;

public final class QuoteVoucherService implements QuoteVoucherUseCase {

  private final BookingIdentityPort accounts;
  private final BookingPricingPort pricing;
  private final VoucherApplicationService vouchers;

  public QuoteVoucherService(
      BookingIdentityPort accounts,
      BookingPricingPort pricing,
      VoucherApplicationService vouchers) {
    this.accounts = accounts;
    this.pricing = pricing;
    this.vouchers = vouchers;
  }

  @Override
  public VoucherQuoteResult execute(UUID customerId, String voucherCode, StayQuoteCriteria stay) {
    accounts
        .findById(customerId)
        .filter(BookingIdentityPort.AccountSnapshot::active)
        .orElseThrow(() -> new BookingException(BookingErrorCode.ACCESS_DENIED));
    var quote = pricing.calculateQuote(stay);
    var applied =
        vouchers.evaluate(voucherCode, quote.hotelId(), customerId, quote.subtotalAmount(), false);
    return new VoucherQuoteResult(
        applied.voucher().code(),
        quote.currency(),
        quote.subtotalAmount(),
        applied.discount(),
        quote.subtotalAmount() - applied.discount());
  }
}
