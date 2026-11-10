package com.team.hotelbooking.hotel.api.controller;

import com.team.hotelbooking.hotel.api.dto.request.CreateHotelRequest;
import com.team.hotelbooking.hotel.api.dto.response.CreateHotelResponse;
import com.team.hotelbooking.hotel.api.dto.response.ManagementHotelResponse;
import com.team.hotelbooking.hotel.application.command.CreateHotelCommand;
import com.team.hotelbooking.hotel.application.result.CreateHotelResult;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;
import com.team.hotelbooking.hotel.application.usecase.ApproveHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.CreateHotelUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/api/v1/management/hotels")
@PreAuthorize("hasAnyRole('ADMIN','HOTEL_OWNER')")
@RequiredArgsConstructor
public class AdminHotelController {

  private final CreateHotelUseCase createHotelUseCase;
  private final ApproveHotelUseCase approveHotelUseCase;

  @PostMapping
  public ResponseEntity<ApiResponse<CreateHotelResponse>> createHotel(
      @Valid @RequestBody CreateHotelRequest request, Principal principal) {

    UUID actorId = UUID.fromString(principal.getName());

    CreateHotelCommand command =
        new CreateHotelCommand(
            actorId,
            request.ownerId(),
            request.cityId(),
            request.name(),
            request.description(),
            request.address(),
            request.latitude(),
            request.longitude(),
            request.starRating(),
            request.checkInTime(),
            request.checkOutTime());

    CreateHotelResult result = createHotelUseCase.execute(command);

    CreateHotelResponse response = CreateHotelResponse.from(result);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tạo khách sạn thành công.", response));
  }

  @Operation(
      summary = "Duyệt công khai khách sạn lần đầu (DRAFT → ACTIVE)",
      description =
          "Chỉ ADMIN. Kiểm HT-03: tên, địa chỉ, ít nhất một liên hệ (điện thoại hoặc email) và ít"
              + " nhất một loại phòng đang bán có sức chứa hợp lệ, có phòng vật lý đang phục vụ."
              + " Thiếu điều kiện trả 422 HOTEL_NOT_READY, liệt kê từng lý do trong errors.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Đã duyệt, trả trạng thái ACTIVE"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Khách sạn không ở trạng thái DRAFT",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Khách sạn không tồn tại",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "422",
        description = "Chưa đủ điều kiện mở bán theo HT-03",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping("/{hotelId}/approve")
  public ResponseEntity<ApiResponse<ManagementHotelResponse>> approveHotel(
      @PathVariable("hotelId") UUID hotelId, Principal principal) {
    UUID adminActorId = UUID.fromString(principal.getName());
    ManagementHotelResult result = approveHotelUseCase.execute(hotelId, adminActorId);
    ManagementHotelResponse response = ManagementHotelResponse.from(result);
    return ResponseEntity.ok().body(ApiResponse.success("Duyệt khách sạn thành công.", response));
  }
}
