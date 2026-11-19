package com.team.hotelbooking.hotel.api.controller;

import com.team.hotelbooking.hotel.api.dto.request.CreateCustomAmenityRequest;
import com.team.hotelbooking.hotel.api.dto.request.UpdateCustomAmenityRequest;
import com.team.hotelbooking.hotel.api.dto.response.CustomAmenityResponse;
import com.team.hotelbooking.hotel.api.dto.response.ManagementAmenitiesResponse;
import com.team.hotelbooking.hotel.application.command.CreateCustomAmenityCommand;
import com.team.hotelbooking.hotel.application.command.DetachCustomAmenityCommand;
import com.team.hotelbooking.hotel.application.command.GetManagementAmenitiesQuery;
import com.team.hotelbooking.hotel.application.command.UpdateCustomAmenityCommand;
import com.team.hotelbooking.hotel.application.usecase.CreateCustomAmenityUseCase;
import com.team.hotelbooking.hotel.application.usecase.DetachCustomAmenityUseCase;
import com.team.hotelbooking.hotel.application.usecase.GetManagementAmenitiesUseCase;
import com.team.hotelbooking.hotel.application.usecase.UpdateCustomAmenityUseCase;
import com.team.hotelbooking.shared.security.CurrentActor;
import com.team.hotelbooking.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({
  "/api/v1/management/hotels/{hotelId}/amenities",
  "/api/v1/management/hotels/{hotelId}/rooms/{roomId}/amenities"
})
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ManagementScopedAmenityController {
  private final GetManagementAmenitiesUseCase getUseCase;
  private final CreateCustomAmenityUseCase createUseCase;
  private final UpdateCustomAmenityUseCase updateUseCase;
  private final DetachCustomAmenityUseCase detachUseCase;

  @Operation(summary = "Đọc tiện ích quản lý của khách sạn hoặc phòng vật lý, kể cả khi còn DRAFT")
  @GetMapping
  public ResponseEntity<ApiResponse<ManagementAmenitiesResponse>> get(
      @PathVariable("hotelId") UUID hotelId,
      @PathVariable(value = "roomId", required = false) UUID roomId,
      Authentication actor) {
    var result =
        getUseCase.execute(
            new GetManagementAmenitiesQuery(
                CurrentActor.userId(actor), CurrentActor.isAdmin(actor), hotelId, roomId));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Lấy tiện ích quản lý thành công.", ManagementAmenitiesResponse.from(result)));
  }

  @Operation(summary = "Tạo và gắn ngay tiện ích riêng, không thêm vào danh mục chung")
  @PostMapping
  public ResponseEntity<ApiResponse<CustomAmenityResponse>> create(
      @PathVariable("hotelId") UUID hotelId,
      @PathVariable(value = "roomId", required = false) UUID roomId,
      @Valid @RequestBody CreateCustomAmenityRequest request,
      Authentication actor) {
    var result =
        createUseCase.execute(
            new CreateCustomAmenityCommand(
                CurrentActor.userId(actor),
                CurrentActor.isAdmin(actor),
                hotelId,
                roomId,
                request.name(),
                request.description()));
    return ResponseEntity.status(HttpStatus.CREATED)
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Tạo tiện ích riêng thành công.", CustomAmenityResponse.from(result)));
  }

  @Operation(summary = "Sửa tên/mô tả hoặc gắn lại tiện ích riêng theo phiên bản đã đọc")
  @PatchMapping("/{amenityId}")
  public ResponseEntity<ApiResponse<CustomAmenityResponse>> update(
      @PathVariable("hotelId") UUID hotelId,
      @PathVariable(value = "roomId", required = false) UUID roomId,
      @PathVariable("amenityId") UUID amenityId,
      @Valid @RequestBody UpdateCustomAmenityRequest request,
      Authentication actor) {
    var result =
        updateUseCase.execute(
            new UpdateCustomAmenityCommand(
                CurrentActor.userId(actor),
                CurrentActor.isAdmin(actor),
                hotelId,
                roomId,
                amenityId,
                request.name(),
                request.description(),
                request.attached(),
                request.expectedVersion()));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Cập nhật tiện ích riêng thành công.", CustomAmenityResponse.from(result)));
  }

  @Operation(
      summary = "Tháo tiện ích riêng; giữ bản ghi để có thể gắn lại bằng PATCH attached=true")
  @DeleteMapping("/{amenityId}")
  public ResponseEntity<ApiResponse<CustomAmenityResponse>> detach(
      @PathVariable("hotelId") UUID hotelId,
      @PathVariable(value = "roomId", required = false) UUID roomId,
      @PathVariable("amenityId") UUID amenityId,
      @RequestParam("expectedVersion") @Min(0) long expectedVersion,
      Authentication actor) {
    var result =
        detachUseCase.execute(
            new DetachCustomAmenityCommand(
                CurrentActor.userId(actor),
                CurrentActor.isAdmin(actor),
                hotelId,
                roomId,
                amenityId,
                expectedVersion));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Tháo tiện ích riêng thành công.", CustomAmenityResponse.from(result)));
  }
}
