package com.team.hotelbooking.hotel.api.controller;

import com.team.hotelbooking.hotel.api.dto.response.ListAmenitiesResponse;
import com.team.hotelbooking.hotel.application.command.GetRoomTypeAmenitiesQuery;
import com.team.hotelbooking.hotel.application.usecase.GetRoomTypeAmenitiesUseCase;
import com.team.hotelbooking.shared.security.CurrentActor;
import com.team.hotelbooking.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/v1/management/hotels/{hotelId}/room-types/{roomTypeId}/amenities")
public class ManagementRoomTypeAmenityQueryController {
  private final GetRoomTypeAmenitiesUseCase getUseCase;

  @GetMapping
  @Operation(
      summary =
          "Đọc tiện ích hạng phòng để quản lý, không yêu cầu khách sạn ACTIVE hay hạng phòng đang bán")
  public ResponseEntity<ApiResponse<List<ListAmenitiesResponse>>> get(
      @PathVariable("hotelId") UUID hotelId,
      @PathVariable("roomTypeId") UUID roomTypeId,
      Authentication actor) {
    var result =
        getUseCase.execute(
            new GetRoomTypeAmenitiesQuery(
                CurrentActor.userId(actor), CurrentActor.isAdmin(actor), hotelId, roomTypeId));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Lấy tiện ích hạng phòng thành công.", ListAmenitiesResponse.from(result)));
  }
}
