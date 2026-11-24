package com.team.hotelbooking.payment.api.controller;

import com.team.hotelbooking.payment.api.dto.response.PartnerCustomerResponse;
import com.team.hotelbooking.payment.application.command.ListPartnerCustomersQuery;
import com.team.hotelbooking.payment.application.usecase.ListPartnerCustomersUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import com.team.hotelbooking.shared.web.PageResponse;
import com.team.hotelbooking.shared.web.PaginationSupport;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PartnerCustomerController {
  private final ListPartnerCustomersUseCase customers;
  private final PaginationSupport pagination;

  @GetMapping("/api/v1/admin/customers")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<PageResponse<PartnerCustomerResponse>>> list(
      @RequestParam(name = "keyword", required = false) @Size(max = 100) String keyword,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size,
      Principal principal) {
    var result =
        customers.execute(
            new ListPartnerCustomersQuery(
                UUID.fromString(principal.getName()), keyword, pagination.resolve(page, size)));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Lấy danh sách owner đã ký hợp đồng thành công.",
                PartnerCustomerResponse.from(result)));
  }
}
