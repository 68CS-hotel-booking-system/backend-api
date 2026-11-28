package com.team.hotelbooking.hotel.api.controller;

import com.team.hotelbooking.hotel.api.dto.request.AddHotelMemberRequest;
import com.team.hotelbooking.hotel.api.dto.request.UpdateHotelMemberStatusRequest;
import com.team.hotelbooking.hotel.api.dto.response.HotelMemberResponse;
import com.team.hotelbooking.hotel.api.dto.response.StaffHotelResponse;
import com.team.hotelbooking.hotel.api.dto.response.WorkplaceResponse;
import com.team.hotelbooking.hotel.application.usecase.HotelStaffUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import com.team.hotelbooking.shared.web.PageResponse;
import com.team.hotelbooking.shared.web.PaginationSupport;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class HotelStaffController {

  private final HotelStaffUseCase staffUseCase;
  private final PaginationSupport paginationSupport;

  @GetMapping("/api/v1/management/staff-hotels")
  public ResponseEntity<ApiResponse<PageResponse<StaffHotelResponse>>> listStaffHotels(
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size,
      Principal principal) {
    var result =
        staffUseCase.listStaffHotels(
            UUID.fromString(principal.getName()), paginationSupport.resolve(page, size));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Lấy danh sách khách sạn phân công nhân sự thành công.",
                StaffHotelResponse.from(result)));
  }

  @GetMapping("/api/v1/management/hotels/{hotelId}/members")
  public ResponseEntity<ApiResponse<PageResponse<HotelMemberResponse>>> listMembers(
      @PathVariable("hotelId") UUID hotelId,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size,
      Principal principal) {
    var result =
        staffUseCase.listMembers(
            UUID.fromString(principal.getName()), hotelId, paginationSupport.resolve(page, size));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Lấy danh sách nhân sự khách sạn thành công.", HotelMemberResponse.from(result)));
  }

  @PostMapping("/api/v1/management/hotels/{hotelId}/members")
  public ResponseEntity<ApiResponse<HotelMemberResponse>> addMember(
      @PathVariable("hotelId") UUID hotelId,
      @Valid @RequestBody AddHotelMemberRequest request,
      Principal principal) {
    var result =
        staffUseCase.addMember(
            UUID.fromString(principal.getName()), hotelId, request.email(), request.memberRole());
    return ResponseEntity.status(HttpStatus.CREATED)
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Phân công nhân sự khách sạn thành công.", HotelMemberResponse.from(result)));
  }

  @PatchMapping("/api/v1/management/hotels/{hotelId}/members/{memberId}")
  public ResponseEntity<ApiResponse<HotelMemberResponse>> updateMemberStatus(
      @PathVariable("hotelId") UUID hotelId,
      @PathVariable("memberId") UUID memberId,
      @Valid @RequestBody UpdateHotelMemberStatusRequest request,
      Principal principal) {
    var result =
        staffUseCase.updateMemberStatus(
            UUID.fromString(principal.getName()),
            hotelId,
            memberId,
            request.status(),
            request.expectedVersion());
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Cập nhật trạng thái phân công nhân sự thành công.",
                HotelMemberResponse.from(result)));
  }

  @GetMapping("/api/v1/me/workplaces")
  public ResponseEntity<ApiResponse<List<WorkplaceResponse>>> listWorkplaces(Principal principal) {
    var result =
        staffUseCase.listWorkplaces(UUID.fromString(principal.getName())).stream()
            .map(WorkplaceResponse::from)
            .toList();
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Lấy danh sách nơi làm việc thành công.", result));
  }
}
