package com.team.hotelbooking.hotel.api.controller;

import com.team.hotelbooking.hotel.api.dto.request.ReplaceAmenitiesRequest;
import com.team.hotelbooking.hotel.api.dto.response.ManagementAmenitiesResponse;
import com.team.hotelbooking.hotel.application.command.ReplaceRoomAmenitiesCommand;
import com.team.hotelbooking.hotel.application.usecase.ReplaceRoomAmenitiesUseCase;
import com.team.hotelbooking.shared.security.CurrentActor;
import com.team.hotelbooking.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/management/hotels/{hotelId}/rooms/{roomId}/amenities")
@PreAuthorize("isAuthenticated()")
public class ManagementRoomAmenityController {
  private final ReplaceRoomAmenitiesUseCase replaceUseCase;

  @Operation(
      summary =
          "Thay danh sách tiện ích có sẵn gắn trực tiếp vào phòng; giữ nguyên tiện ích riêng và tiện ích hạng phòng")
  @PutMapping
  public ResponseEntity<ApiResponse<ManagementAmenitiesResponse>> replace(
      @PathVariable("hotelId") UUID hotelId,
      @PathVariable("roomId") UUID roomId,
      @Valid @RequestBody ReplaceAmenitiesRequest request,
      Authentication actor) {
    var result =
        replaceUseCase.execute(
            new ReplaceRoomAmenitiesCommand(
                CurrentActor.userId(actor),
                CurrentActor.isAdmin(actor),
                hotelId,
                roomId,
                request.amenityIds()));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Cập nhật tiện ích phòng vật lý thành công.",
                ManagementAmenitiesResponse.from(result)));
  }
}
