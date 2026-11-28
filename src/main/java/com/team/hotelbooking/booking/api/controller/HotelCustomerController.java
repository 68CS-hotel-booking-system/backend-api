package com.team.hotelbooking.booking.api.controller;

import com.team.hotelbooking.booking.api.dto.response.HotelCustomerResponse;
import com.team.hotelbooking.booking.application.command.ListHotelCustomersQuery;
import com.team.hotelbooking.booking.application.usecase.ListHotelCustomersUseCase;
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
public class HotelCustomerController {
  private final ListHotelCustomersUseCase customers;
  private final PaginationSupport pagination;

  @GetMapping("/api/v1/management/customers")
  @PreAuthorize("hasRole('HOTEL_OWNER')")
  public ResponseEntity<ApiResponse<PageResponse<HotelCustomerResponse>>> list(
      @RequestParam(name = "keyword", required = false) @Size(max = 100) String keyword,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size,
      Principal principal) {
    var result =
        customers.execute(
            new ListHotelCustomersQuery(
                UUID.fromString(principal.getName()), keyword, pagination.resolve(page, size)));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Lấy danh sách khách đặt phòng thành công.", HotelCustomerResponse.from(result)));
  }
}
