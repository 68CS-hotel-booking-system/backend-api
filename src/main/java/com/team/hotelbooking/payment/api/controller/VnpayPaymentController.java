package com.team.hotelbooking.payment.api.controller;

import com.team.hotelbooking.payment.application.result.PaymentResult;
import com.team.hotelbooking.payment.application.result.VnpayIpnResult;
import com.team.hotelbooking.payment.application.service.VnpayPaymentService;
import com.team.hotelbooking.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments - VNPAY")
public class VnpayPaymentController {
  private static final Logger log = LoggerFactory.getLogger(VnpayPaymentController.class);
  private static final Pattern TXN_REF =
      Pattern.compile("[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}");
  private final VnpayPaymentService payments;

  @PostMapping("/bookings/{bookingId}/vnpay")
  @Operation(summary = "Tạo hoặc lấy lại URL VNPAY đang chờ của đơn thuộc tài khoản hiện tại")
  public ResponseEntity<ApiResponse<PaymentResult>> create(
      @PathVariable("bookingId") UUID bookingId,
      @RequestParam(name = "qr", defaultValue = "false") boolean qr,
      Principal principal,
      HttpServletRequest request) {
    String ip = request.getRemoteAddr();
    if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) ip = "127.0.0.1";
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                payments.create(UUID.fromString(principal.getName()), bookingId, ip, qr)));
  }

  @GetMapping("/{paymentId}")
  @Operation(summary = "Đọc trạng thái thanh toán của chính tài khoản hiện tại")
  public ResponseEntity<ApiResponse<PaymentResult>> get(
      @PathVariable("paymentId") UUID paymentId, Principal principal) {
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success(payments.get(UUID.fromString(principal.getName()), paymentId)));
  }

  @GetMapping("/vnpay/ipn")
  @Operation(summary = "IPN server-to-server của VNPAY, xác thực bằng chữ ký HMAC-SHA512")
  public ResponseEntity<VnpayIpnResult> ipn(
      @RequestParam MultiValueMap<String, String> parameters) {
    VnpayIpnResult result;
    try {
      result = payments.receiveIpn(parameters);
    } catch (RuntimeException exception) {
      log.error("Không thể lưu IPN VNPAY; yêu cầu sẽ được gọi lại.", exception);
      result = VnpayIpnResult.of("99");
    }
    var references = parameters.get("vnp_TxnRef");
    String reference = references != null && references.size() == 1 ? references.getFirst() : null;
    log.info(
        "VNPAY IPN txnRef={} rspCode={}",
        reference != null && TXN_REF.matcher(reference).matches() ? reference : "(missing/invalid)",
        result.code());
    return ResponseEntity.ok().header("Cache-Control", "no-store").body(result);
  }
}
