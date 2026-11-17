package com.team.hotelbooking.payment.infrastructure.integration;

import static org.assertj.core.api.Assertions.*;

import com.team.hotelbooking.payment.infrastructure.config.VnpayProperties;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VnpayGatewayAdapterTests {
  private final VnpayGatewayAdapter gateway =
      new VnpayGatewayAdapter(
          new VnpayProperties(
              true,
              "TESTCODE",
              "sandbox-test-secret",
              "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
              "http://localhost:5173/payment/return"));

  @Test
  void verifiesIndependentNodeHmacVectorAndRejectsTamperedAmount() {
    var parameters =
        new HashMap<>(
            Map.of(
                "vnp_Amount",
                "12345600",
                "vnp_OrderInfo",
                "Thanh toan BK/123",
                "vnp_TmnCode",
                "TESTCODE",
                "vnp_TxnRef",
                "11111111-1111-4111-8111-111111111111",
                "vnp_SecureHash",
                "7350c8212de801ca3a01f9c5fc1a29d531789e1b0febd7e8f26f05132d32b2a8026f4f382959fd545e8687c1520c2ce48bbb08f756a380d961def07f52ee4f9c"));
    assertThat(gateway.verify(parameters)).isTrue();
    parameters.put("vnp_Amount", "1");
    assertThat(gateway.verify(parameters)).isFalse();
    parameters.put("vnp_SecureHash", "invalid");
    assertThat(gateway.verify(parameters)).isFalse();
  }

  @Test
  void createsSignedUrlWithAmountTimes100AndVietnamTime() {
    UUID id = UUID.randomUUID();
    String url =
        gateway.createUrl(
            id,
            123456,
            "BK/123",
            Instant.parse("2026-11-15T03:00:00Z"),
            Instant.parse("2026-11-15T03:15:00Z"),
            "127.0.0.1");
    Map<String, String> parameters = new HashMap<>();
    for (String part : url.substring(url.indexOf('?') + 1).split("&")) {
      String[] pair = part.split("=", 2);
      parameters.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
    }
    assertThat(parameters)
        .containsEntry("vnp_Amount", "12345600")
        .containsEntry("vnp_TxnRef", id.toString())
        .containsEntry("vnp_CreateDate", "20261115100000")
        .containsEntry("vnp_ExpireDate", "20261115101500")
        .containsEntry("vnp_ReturnUrl", "http://localhost:5173/payment/return");
    assertThat(gateway.verify(parameters)).isTrue();
  }

  @Test
  void disabledGatewayRejectsCallbacksAndInvalidEnabledConfigFailsEarly() {
    var disabled = new VnpayGatewayAdapter(new VnpayProperties(false, "", "", "", ""));
    assertThat(disabled.verify(Map.of())).isFalse();
    assertThat(disabled.matchesMerchant("TESTCODE")).isFalse();
    assertThatThrownBy(() -> new VnpayProperties(true, "", "", "", ""))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new VnpayProperties(
                    true,
                    "TEST",
                    "secret",
                    "https://evil.example/",
                    "http://localhost:5173/payment/return"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(gateway.matchesMerchant("OTHER")).isFalse();
  }
}
