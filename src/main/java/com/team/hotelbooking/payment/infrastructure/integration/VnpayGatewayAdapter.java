package com.team.hotelbooking.payment.infrastructure.integration;

import com.team.hotelbooking.payment.application.port.VnpayGateway;
import com.team.hotelbooking.payment.domain.exception.FinanceErrorCode;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.infrastructure.config.VnpayProperties;
import com.team.hotelbooking.shared.time.BusinessTime;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class VnpayGatewayAdapter implements VnpayGateway {
  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(BusinessTime.ZONE);
  private final VnpayProperties properties;
  private final HttpClient http;
  private final JsonMapper json = JsonMapper.builder().build();

  @Autowired
  public VnpayGatewayAdapter(VnpayProperties properties) {
    this(properties, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
  }

  VnpayGatewayAdapter(VnpayProperties properties, HttpClient http) {
    this.properties = properties;
    this.http = http;
  }

  @Override
  public boolean enabled() {
    return properties.enabled();
  }

  @Override
  public boolean matchesMerchant(String tmnCode) {
    return enabled() && properties.tmnCode().equals(tmnCode);
  }

  @Override
  public String createUrl(
      UUID paymentId,
      long amountVnd,
      String bookingCode,
      Instant createdAt,
      Instant expiresAt,
      String ipAddress) {
    return signedUrl("pay", paymentId, amountVnd, bookingCode, createdAt, expiresAt, ipAddress);
  }

  @Override
  public String createQr(
      UUID paymentId,
      long amountVnd,
      String bookingCode,
      Instant createdAt,
      Instant expiresAt,
      String ipAddress) {
    var uri =
        URI.create(
            signedUrl("genqr", paymentId, amountVnd, bookingCode, createdAt, expiresAt, ipAddress));
    var request =
        HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(5))
            .header("Accept", "application/json")
            .GET()
            .build();
    try {
      var response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() != 200
          || response.body() == null
          || response.body().length() > 16384) {
        throw new FinanceException(FinanceErrorCode.PAYMENT_QR_UNAVAILABLE);
      }
      var body = json.readTree(response.body());
      var content = body.path("qrcontent");
      if (!"00".equals(body.path("code").asText())
          || !content.isString()
          || content.asText().isBlank()
          || content.asText().length() > 2048) {
        throw new FinanceException(FinanceErrorCode.PAYMENT_QR_UNAVAILABLE);
      }
      return content.asText();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new FinanceException(FinanceErrorCode.PAYMENT_QR_UNAVAILABLE);
    } catch (IOException | tools.jackson.core.JacksonException exception) {
      throw new FinanceException(FinanceErrorCode.PAYMENT_QR_UNAVAILABLE);
    }
  }

  private String signedUrl(
      String command,
      UUID paymentId,
      long amountVnd,
      String bookingCode,
      Instant createdAt,
      Instant expiresAt,
      String ipAddress) {
    if (!enabled()) throw new IllegalStateException("VNPAY chưa được bật.");
    Map<String, String> parameters = new TreeMap<>();
    parameters.put("vnp_Version", "2.1.0");
    parameters.put("vnp_Command", command);
    parameters.put("vnp_TmnCode", properties.tmnCode());
    parameters.put("vnp_Amount", Long.toString(Math.multiplyExact(amountVnd, 100)));
    parameters.put("vnp_CurrCode", "VND");
    parameters.put("vnp_TxnRef", paymentId.toString());
    parameters.put("vnp_OrderInfo", "Thanh toan dat phong " + bookingCode);
    parameters.put("vnp_OrderType", "other");
    parameters.put("vnp_Locale", "vn");
    parameters.put("vnp_ReturnUrl", properties.returnUrl());
    parameters.put("vnp_IpAddr", ipAddress);
    parameters.put("vnp_CreateDate", DATE.format(createdAt));
    parameters.put("vnp_ExpireDate", DATE.format(expiresAt));
    String query = canonical(parameters);
    return properties.paymentUrl()
        + "?"
        + query
        + "&vnp_SecureHash="
        + HexFormat.of().formatHex(hmac(query));
  }

  @Override
  public boolean verify(Map<String, String> parameters) {
    String signature = parameters.get("vnp_SecureHash");
    if (!enabled() || signature == null || !signature.matches("[a-fA-F0-9]{128}")) return false;
    return MessageDigest.isEqual(hmac(canonical(parameters)), HexFormat.of().parseHex(signature));
  }

  private byte[] hmac(String query) {
    try {
      Mac mac = Mac.getInstance("HmacSHA512");
      mac.init(
          new SecretKeySpec(
              properties.hashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
      return mac.doFinal(query.getBytes(StandardCharsets.UTF_8));
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("Không thể ký yêu cầu VNPAY.", exception);
    }
  }

  private static String canonical(Map<String, String> parameters) {
    return new TreeMap<>(parameters)
        .entrySet().stream()
            .filter(
                e ->
                    e.getKey().startsWith("vnp_")
                        && !e.getKey().equals("vnp_SecureHash")
                        && !e.getKey().equals("vnp_SecureHashType"))
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
            .map(
                e ->
                    URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
  }
}
