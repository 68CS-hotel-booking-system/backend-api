package com.team.hotelbooking.booking.application.result;

public record VoucherQuoteResult(
    String voucherCode,
    String currency,
    long subtotalAmount,
    long discountAmount,
    long totalAmount) {}
