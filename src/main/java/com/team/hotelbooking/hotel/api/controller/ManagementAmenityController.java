package com.team.hotelbooking.hotel.api.controller;

import com.team.hotelbooking.hotel.api.dto.request.ReplaceAmenitiesRequest;
import com.team.hotelbooking.hotel.api.dto.response.ListAmenitiesResponse;
import com.team.hotelbooking.hotel.application.command.ReplaceHotelAmenitiesCommand;
import com.team.hotelbooking.hotel.application.command.ReplaceRoomTypeAmenitiesCommand;
import com.team.hotelbooking.hotel.application.result.ListAmenitiesResult;
import com.team.hotelbooking.hotel.application.usecase.ReplaceHotelAmenitiesUseCase;
import com.team.hotelbooking.hotel.application.usecase.ReplaceRoomTypeAmenitiesUseCase;
import com.team.hotelbooking.shared.security.CurrentActor;
import com.team.hotelbooking.shared.web.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Cùng quy tắc quyền với PATCH thông tin khách sạn: ADMIN, Owner hoặc HOTEL_MANAGER của đúng
// hotel; RECEPTIONIST của hotel → 403; người ngoài hoặc hotel không tồn tại → 404.
@RestController
@RequestMapping("/api/v1/management/hotels/{hotelId}")
@RequiredArgsConstructor
public class ManagementAmenityController {

  private final ReplaceHotelAmenitiesUseCase replaceHotelAmenitiesUseCase;
  private final ReplaceRoomTypeAmenitiesUseCase replaceRoomTypeAmenitiesUseCase;

  @PutMapping("/amenities")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<List<ListAmenitiesResponse>>> replaceHotelAmenities(
      @PathVariable("hotelId") UUID hotelId,
      @Valid @RequestBody ReplaceAmenitiesRequest request,
      Authentication authentication) {
    ReplaceHotelAmenitiesCommand command =
        new ReplaceHotelAmenitiesCommand(
            CurrentActor.userId(authentication),
            CurrentActor.isAdmin(authentication),
            hotelId,
            request.amenityIds());

    List<ListAmenitiesResult> result = replaceHotelAmenitiesUseCase.execute(command);

    List<ListAmenitiesResponse> response = ListAmenitiesResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Cập nhật tiện ích khách sạn thành công.", response));
  }

  @PutMapping("/room-types/{roomTypeId}/amenities")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<List<ListAmenitiesResponse>>> replaceRoomTypeAmenities(
      @PathVariable("hotelId") UUID hotelId,
      @PathVariable("roomTypeId") UUID roomTypeId,
      @Valid @RequestBody ReplaceAmenitiesRequest request,
      Authentication authentication) {
    ReplaceRoomTypeAmenitiesCommand command =
        new ReplaceRoomTypeAmenitiesCommand(
            CurrentActor.userId(authentication),
            CurrentActor.isAdmin(authentication),
            hotelId,
            roomTypeId,
            request.amenityIds());

    List<ListAmenitiesResult> result = replaceRoomTypeAmenitiesUseCase.execute(command);

    List<ListAmenitiesResponse> response = ListAmenitiesResponse.from(result);

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Cập nhật tiện ích loại phòng thành công.", response));
  }
}
