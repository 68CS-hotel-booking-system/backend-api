package com.team.hotelbooking.payment.api.controller;

import com.team.hotelbooking.payment.api.dto.response.RefundResponse;
import com.team.hotelbooking.payment.application.command.*;
import com.team.hotelbooking.payment.application.usecase.*;
import com.team.hotelbooking.payment.domain.model.Refund;
import com.team.hotelbooking.shared.web.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class RefundController {
  private final ListRefundsUseCase list;
  private final GetRefundUseCase get;
  private final ProcessRefundUseCase process;
  private final PaginationSupport pagination;

  @GetMapping({"/api/v1/management/refunds", "/api/v1/me/refunds"})
  public ResponseEntity<ApiResponse<PageResponse<RefundResponse>>> list(
      @RequestParam(name = "status", required = false) Refund.Status status,
      @RequestParam(name = "bookingId", required = false) UUID bookingId,
      @RequestParam(name = "keyword", required = false) @Size(max = 100) String keyword,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size,
      jakarta.servlet.http.HttpServletRequest request,
      Principal principal) {
    var result =
        list.execute(
            new ListRefundsQuery(
                UUID.fromString(principal.getName()),
                request.getRequestURI().contains("/management/"),
                status,
                bookingId,
                keyword,
                pagination.resolve(page, size)));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Lấy danh sách hoàn tiền thành công.",
                PageResponse.from(result.map(RefundResponse::from))));
  }

  @GetMapping({"/api/v1/management/refunds/{id}", "/api/v1/me/refunds/{id}"})
  public ResponseEntity<ApiResponse<RefundResponse>> get(
      @PathVariable("id") UUID id,
      jakarta.servlet.http.HttpServletRequest request,
      Principal principal) {
    var result =
        get.execute(
            new GetRefundQuery(
                UUID.fromString(principal.getName()),
                request.getRequestURI().contains("/management/"),
                id));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success("Lấy chi tiết hoàn tiền thành công.", RefundResponse.from(result)));
  }

  public record ActionRequest(
      @NotNull Refund.Status status,
      @Size(max = 100) String reference,
      @NotBlank @Size(max = 1000) String note,
      @NotNull @Min(0) Long expectedVersion) {}

  @PostMapping("/api/v1/admin/refunds/{id}/actions")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<RefundResponse>> process(
      @PathVariable("id") UUID id, @Valid @RequestBody ActionRequest body, Principal principal) {
    var result =
        process.execute(
            new ProcessRefundCommand(
                UUID.fromString(principal.getName()),
                id,
                body.status(),
                body.reference(),
                body.note(),
                body.expectedVersion()));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success("Đã cập nhật tiến trình hoàn tiền.", RefundResponse.from(result)));
  }
}
