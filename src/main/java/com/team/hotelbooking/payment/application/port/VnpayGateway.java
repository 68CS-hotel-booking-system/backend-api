package com.team.hotelbooking.payment.application.port;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public interface VnpayGateway {
  boolean enabled();

  boolean matchesMerchant(String tmnCode);

  boolean verify(Map<String, String> parameters);

  String createUrl(
      UUID paymentId,
      long amountVnd,
      String bookingCode,
      Instant createdAt,
      Instant expiresAt,
      String ipAddress);
}
