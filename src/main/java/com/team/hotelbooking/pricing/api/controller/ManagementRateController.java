package com.team.hotelbooking.pricing.api.controller;

import com.team.hotelbooking.pricing.api.dto.response.DailyRateDto;
import com.team.hotelbooking.pricing.api.dto.response.RoomTypeRatesResponse;
import com.team.hotelbooking.pricing.application.command.GetRoomRatesQuery;
import com.team.hotelbooking.pricing.application.usecase.GetRoomRatesUseCase;
import com.team.hotelbooking.pricing.domain.model.RoomTypeRates;
import com.team.hotelbooking.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/management/hotels")
@RequiredArgsConstructor
@Validated
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
      @Parameter(description = "Ngày bắt đầu (YYYY-MM-DD)", required = true)
          @RequestParam
          @NotNull(message = "Ngày bắt đầu không được để trống.")
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @Parameter(description = "Ngày kết thúc (YYYY-MM-DD)", required = true)
          @RequestParam
          @NotNull(message = "Ngày kết thúc không được để trống.")
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate) {

    RoomTypeRates result =
        getRoomRatesUseCase.execute(new GetRoomRatesQuery(hotelId, roomTypeId, startDate, endDate));

    List<DailyRateDto> dtos =
        result.rates().stream()
            .map(r -> new DailyRateDto(r.date(), r.price(), r.hasRate(), r.updatedAt()))
            .toList();

    RoomTypeRatesResponse response =
        new RoomTypeRatesResponse(
            result.hotelId(),
            result.roomTypeId(),
            result.currency(),
            result.startDate(),
            result.endDate(),
            dtos);

    return ResponseEntity.ok(ApiResponse.success("Thành công.", response));
  }
}
