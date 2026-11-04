package com.team.hotelbooking.hotel.api.controller;

import com.team.hotelbooking.hotel.api.dto.response.ManagementHotelResponse;
import com.team.hotelbooking.hotel.application.command.GetManagementHotelQuery;
import com.team.hotelbooking.hotel.application.command.ListManagementHotelsQuery;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.application.usecase.GetManagementHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListManagementHotelsUseCase;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.web.ApiResponse;
import com.team.hotelbooking.shared.web.PageResponse;
import com.team.hotelbooking.shared.web.PaginationSupport;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/management/hotels")
@PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER')")
@RequiredArgsConstructor
public class ManagementHotelController {

  private final GetManagementHotelUseCase getManagementHotelUseCase;
  private final ListManagementHotelsUseCase listManagementHotelsUseCase;
  private final PaginationSupport paginationSupport;

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<ManagementHotelResponse>>> listHotels(
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size,
      Principal principal) {
    PageQuery pagination = paginationSupport.resolve(page, size);
    ListManagementHotelsQuery query =
        new ListManagementHotelsQuery(UUID.fromString(principal.getName()), pagination);

    PageResult<ManagementHotelResult> result = listManagementHotelsUseCase.execute(query);

    PageResponse<ManagementHotelResponse> response = ManagementHotelResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Lấy danh sách khách sạn quản lý thành công.", response));
  }

  @GetMapping("/{hotelId}")
  public ResponseEntity<ApiResponse<ManagementHotelResponse>> getHotel(
      @PathVariable("hotelId") UUID hotelId, Principal principal) {
    GetManagementHotelQuery query =
        new GetManagementHotelQuery(UUID.fromString(principal.getName()), hotelId);

    ManagementHotelResult result = getManagementHotelUseCase.execute(query);

    ManagementHotelResponse response = ManagementHotelResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Lấy thông tin quản lý khách sạn thành công.", response));
  }
}
