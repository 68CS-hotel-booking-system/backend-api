package com.team.hotelbooking.payment.infrastructure.integration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.team.hotelbooking.payment.domain.exception.FinanceErrorCode;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.infrastructure.config.VnpayProperties;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class VnpayGatewayAdapterTests {
  @Test
  @SuppressWarnings("unchecked")
  void requestsSignedMerchantHostedQrWithoutFollowingPaymentPage() throws Exception {
    var http = mock(HttpClient.class);
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body())
        .thenReturn("{\"code\":\"00\",\"message\":\"Success\",\"qrcontent\":\"000201-test-qr\"}");
    when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(response);
    var qrGateway =
        new VnpayGatewayAdapter(
            new VnpayProperties(
                true,
                "TESTCODE",
                "sandbox-test-secret",
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                "http://localhost:5173/payment/return"),
            http);
    var id = UUID.randomUUID();
    var now = Instant.parse("2026-11-15T03:00:00Z");
    assertThat(qrGateway.createQr(id, 123456, "BK/123", now, now.plusSeconds(900), "127.0.0.1"))
        .isEqualTo("000201-test-qr");
    var request = ArgumentCaptor.forClass(HttpRequest.class);
    verify(http).send(request.capture(), any(HttpResponse.BodyHandler.class));
    var parameters = new HashMap<String, String>();
    for (var part : request.getValue().uri().getRawQuery().split("&")) {
      var pair = part.split("=", 2);
      parameters.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
    }
    assertThat(parameters)
        .containsEntry("vnp_Command", "genqr")
        .containsEntry("vnp_Amount", "12345600")
        .containsEntry("vnp_TxnRef", id.toString());
    assertThat(qrGateway.verify(parameters)).isTrue();
    assertThat(request.getValue().timeout()).contains(java.time.Duration.ofSeconds(5));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"code\":\"02\",\"qrcontent\":\"\"}",
        "{\"code\":\"00\",\"qrcontent\":\"\"}",
        "{\"code\":\"00\",\"qrcontent\":123}",
        "<html>payment page</html>",
        "null"
      })
  @SuppressWarnings("unchecked")
  void rejectsProviderErrorsAndMalformedQrResponses(String body) throws Exception {
    var http = mock(HttpClient.class);
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn(body);
    when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(response);
    var qrGateway =
        new VnpayGatewayAdapter(
            new VnpayProperties(
                true,
                "TESTCODE",
                "secret",
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                "http://localhost:5173/payment/return"),
            http);
    assertThatThrownBy(
            () ->
                qrGateway.createQr(
                    UUID.randomUUID(),
                    5000,
                    "BK",
                    Instant.now(),
                    Instant.now().plusSeconds(900),
                    "127.0.0.1"))
        .isInstanceOfSatisfying(
            FinanceException.class,
            error -> assertThat(error.code()).isEqualTo(FinanceErrorCode.PAYMENT_QR_UNAVAILABLE));
  }

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
