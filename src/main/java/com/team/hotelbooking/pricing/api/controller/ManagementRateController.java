package com.team.hotelbooking.pricing.api.controller;

import com.team.hotelbooking.pricing.api.dto.request.GetRoomRatesRequest;
import com.team.hotelbooking.pricing.api.dto.response.RoomTypeRatesResponse;
import com.team.hotelbooking.pricing.application.result.RoomTypeRatesResult;
import com.team.hotelbooking.pricing.application.usecase.GetRoomRatesUseCase;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/management/hotels")
@RequiredArgsConstructor
@Tag(name = "Pricing - Rates", description = "API quản lý giá phòng theo ngày")
public class ManagementRateController {

  private final GetRoomRatesUseCase getRoomRatesUseCase;

  @Operation(
      summary = "[PR01] Tra cứu giá phòng chi tiết theo dải ngày",
      description =
          "Lấy danh sách giá từng ngày cho loại phòng trong khoảng từ startDate đến endDate (tối đa 90 ngày). "
              + "Nếu ngày nào chưa được thiết lập giá riêng, trường hasRate sẽ mang giá trị false.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Tra cứu thành công"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Dải ngày không hợp lệ hoặc vượt quá 90 ngày",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Không tìm thấy khách sạn hoặc loại phòng",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  @GetMapping("/{hotelId}/room-types/{roomTypeId}/rates")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<RoomTypeRatesResponse>> getRoomRates(
      @Parameter(description = "ID khách sạn", required = true) @PathVariable UUID hotelId,
      @Parameter(description = "ID loại phòng", required = true) @PathVariable UUID roomTypeId,
      @ParameterObject @Valid GetRoomRatesRequest request) {

    RoomTypeRatesResult result = getRoomRatesUseCase.execute(request.toQuery(hotelId, roomTypeId));

    RoomTypeRatesResponse response = RoomTypeRatesResponse.from(result);

    return ResponseEntity.ok(ApiResponse.success("Thành công.", response));
  }
}
