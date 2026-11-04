package com.team.hotelbooking.hotel.api.controller;

import com.team.hotelbooking.hotel.api.dto.request.CreateRoomTypeRequest;
import com.team.hotelbooking.hotel.api.dto.request.UpdateRoomTypeRequest;
import com.team.hotelbooking.hotel.api.dto.response.CreateRoomTypeResponse;
import com.team.hotelbooking.hotel.api.dto.response.RoomTypeResponse;
import com.team.hotelbooking.hotel.application.command.CreateRoomTypeCommand;
import com.team.hotelbooking.hotel.application.result.CreateRoomTypeResult;
import com.team.hotelbooking.hotel.application.result.RoomTypeResult;
import com.team.hotelbooking.hotel.application.usecase.CreateRoomTypeUseCase;
import com.team.hotelbooking.hotel.application.usecase.UpdateRoomTypeUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/management/hotels/{hotelId}/room-types")
@RequiredArgsConstructor
@Tag(name = "Hotel Management - Room Types", description = "API quản lý loại phòng khách sạn")
public class ManagementRoomTypeController {

  private final CreateRoomTypeUseCase createRoomTypeUseCase;
  private final UpdateRoomTypeUseCase updateRoomTypeUseCase;

  @Operation(summary = "Tạo mới loại phòng cho khách sạn")
  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER')")
  public ResponseEntity<ApiResponse<CreateRoomTypeResponse>> createRoomType(
      @PathVariable("hotelId") UUID hotelId,
      @Valid @RequestBody CreateRoomTypeRequest request,
      Principal principal) {
    CreateRoomTypeCommand command =
        new CreateRoomTypeCommand(
            UUID.fromString(principal.getName()),
            hotelId,
            request.name(),
            request.description(),
            request.maxAdults(),
            request.maxChildren());

    CreateRoomTypeResult result = createRoomTypeUseCase.execute(command);

    CreateRoomTypeResponse response = CreateRoomTypeResponse.from(result);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tạo loại phòng thành công.", response));
  }

  @Operation(
      summary = "[HT17] Cập nhật thông tin hoặc ngừng bán loại phòng",
      description =
          "Chỉnh sửa một phần thông tin loại phòng (tên, mô tả, sức chứa, giá cơ bản) hoặc cập nhật trạng thái mở bán (active).")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Cập nhật thành công"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Thông tin cập nhật không hợp lệ",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Không tìm thấy khách sạn hoặc loại phòng",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  @PatchMapping("/{roomTypeId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<RoomTypeResponse>> updateRoomType(
      @Parameter(description = "ID khách sạn", required = true) @PathVariable UUID hotelId,
      @Parameter(description = "ID loại phòng", required = true) @PathVariable UUID roomTypeId,
      @Valid @RequestBody UpdateRoomTypeRequest request) {

    RoomTypeResult result = updateRoomTypeUseCase.execute(request.toCommand(hotelId, roomTypeId));

    return ResponseEntity.ok(
        ApiResponse.success("Cập nhật loại phòng thành công.", RoomTypeResponse.from(result)));
  }
}
