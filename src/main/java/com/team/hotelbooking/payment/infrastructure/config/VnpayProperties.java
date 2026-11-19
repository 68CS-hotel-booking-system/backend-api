package com.team.hotelbooking.payment.infrastructure.config;

import java.net.URI;
import java.util.Objects;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("payment.vnpay")
public record VnpayProperties(
    boolean enabled, String tmnCode, String hashSecret, String paymentUrl, String returnUrl) {
  public VnpayProperties {
    if (enabled) {
      if (tmnCode == null || tmnCode.isBlank() || hashSecret == null || hashSecret.isBlank()) {
        throw new IllegalArgumentException("VNPAY đã bật nhưng thiếu TmnCode hoặc HashSecret.");
      }
      URI pay = URI.create(Objects.requireNonNullElse(paymentUrl, ""));
      if (!"https".equals(pay.getScheme())
          || pay.getHost() == null
          || !Set.of("sandbox.vnpayment.vn", "pay.vnpay.vn").contains(pay.getHost())
          || pay.getUserInfo() != null
          || pay.getQuery() != null
          || pay.getFragment() != null) {
        throw new IllegalArgumentException("VNPAY_PAYMENT_URL không hợp lệ.");
      }
      URI back = URI.create(Objects.requireNonNullElse(returnUrl, ""));
      boolean local =
          "http".equals(back.getScheme())
              && back.getHost() != null
              && Set.of("localhost", "127.0.0.1").contains(back.getHost());
      if ((!"https".equals(back.getScheme()) && !local)
          || back.getHost() == null
          || back.getUserInfo() != null
          || back.getQuery() != null
          || back.getFragment() != null) {
        throw new IllegalArgumentException("VNPAY_RETURN_URL phải là HTTPS hoặc HTTP localhost.");
      }
    }
  }

  @Override
  public String toString() {
    return "VnpayProperties[enabled=" + enabled + "]";
  }
}
