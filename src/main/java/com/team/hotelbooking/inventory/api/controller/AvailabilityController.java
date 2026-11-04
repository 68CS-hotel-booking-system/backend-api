package com.team.hotelbooking.inventory.api.controller;

import com.team.hotelbooking.inventory.api.dto.request.CheckAvailabilityRequest;
import com.team.hotelbooking.inventory.api.dto.response.HotelAvailabilityResponse;
import com.team.hotelbooking.inventory.application.result.CheckAvailabilityResult;
import com.team.hotelbooking.inventory.application.usecase.CheckAvailabilityUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hotels")
@RequiredArgsConstructor
@Tag(
    name = "Inventory - Availability",
    description = "API tra cứu tình trạng phòng trống khách sạn")
public class AvailabilityController {

  private final CheckAvailabilityUseCase checkAvailabilityUseCase;

  @Operation(
      summary = "[IV01] Tra cứu tình trạng phòng trống và báo giá ước tính",
      description =
          "Tính toán số lượng phòng khả dụng theo từng đêm trong khoảng lưu trú [checkIn, checkOut) "
              + "dựa trên tổng số phòng vật lý trừ đi số phòng bị chặn (room_blocks), đang giữ (held) và đã xác nhận (confirmed).")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Tra cứu thành công"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Khoảng thời gian không hợp lệ hoặc vượt quá 30 đêm",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Khách sạn không tồn tại hoặc chưa mở bán",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  @GetMapping("/{hotelId}/availability")
  public ResponseEntity<ApiResponse<HotelAvailabilityResponse>> getHotelAvailability(
      @Parameter(description = "ID khách sạn", required = true) @PathVariable UUID hotelId,
      @ParameterObject @Valid CheckAvailabilityRequest request) {

    CheckAvailabilityResult result = checkAvailabilityUseCase.execute(request.toQuery(hotelId));

    HotelAvailabilityResponse response = HotelAvailabilityResponse.from(result);

    return ResponseEntity.ok(ApiResponse.success("Thành công.", response));
  }
}
