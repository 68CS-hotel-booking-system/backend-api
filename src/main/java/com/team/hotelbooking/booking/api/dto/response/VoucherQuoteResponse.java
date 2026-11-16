package com.team.hotelbooking.booking.api.dto.response;

import com.team.hotelbooking.booking.application.result.VoucherQuoteResult;

public record VoucherQuoteResponse(
    String voucherCode,
    String currency,
    long subtotalAmount,
    long discountAmount,
    long totalAmount) {

  public static VoucherQuoteResponse from(VoucherQuoteResult result) {
    return new VoucherQuoteResponse(
        result.voucherCode(),
        result.currency(),
        result.subtotalAmount(),
        result.discountAmount(),
        result.totalAmount());
  }
}
