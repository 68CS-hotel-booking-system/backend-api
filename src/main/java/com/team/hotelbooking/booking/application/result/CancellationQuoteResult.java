package com.team.hotelbooking.booking.application.result;

import java.time.Instant;

public record CancellationQuoteResult(
    boolean canCancel,
    String message,
    long expectedVersion,
    long refundAmount,
    String currency,
    Instant cutoff) {}
