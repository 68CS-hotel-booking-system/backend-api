package com.team.hotelbooking.inventory.api.controller;

import com.team.hotelbooking.inventory.api.dto.response.HotelAvailabilityResponse;
import com.team.hotelbooking.inventory.api.dto.response.RoomTypeAvailabilityDto;
import com.team.hotelbooking.inventory.application.command.CheckAvailabilityQuery;
import com.team.hotelbooking.inventory.application.usecase.CheckAvailabilityUseCase;
import com.team.hotelbooking.inventory.domain.model.HotelAvailability;
import com.team.hotelbooking.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hotels")
@RequiredArgsConstructor
@Validated
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
      @Parameter(description = "Ngày nhận phòng (YYYY-MM-DD)", required = true)
          @RequestParam
          @NotNull(message = "Ngày nhận phòng không được để trống.")
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate checkInDate,
      @Parameter(description = "Ngày trả phòng (YYYY-MM-DD)", required = true)
          @RequestParam
          @NotNull(message = "Ngày trả phòng không được để trống.")
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate checkOutDate,
      @Parameter(description = "Số lượng người lớn (mặc định: 1)")
          @RequestParam(required = false, defaultValue = "1")
          @Min(value = 1, message = "Số lượng người lớn tối thiểu là 1.")
          Integer adults,
      @Parameter(description = "Số lượng trẻ em (mặc định: 0)")
          @RequestParam(required = false, defaultValue = "0")
          @Min(value = 0, message = "Số lượng trẻ em không được âm.")
          Integer children,
      @Parameter(description = "Số lượng phòng cần đặt (mặc định: 1)")
          @RequestParam(required = false, defaultValue = "1")
          @Min(value = 1, message = "Số lượng phòng tối thiểu là 1.")
          Integer quantity) {

    HotelAvailability availability =
        checkAvailabilityUseCase.execute(
            new CheckAvailabilityQuery(
                hotelId, checkInDate, checkOutDate, adults, children, quantity));

    List<RoomTypeAvailabilityDto> roomTypeDtos =
        availability.roomTypes().stream()
            .map(
                rt ->
                    new RoomTypeAvailabilityDto(
                        rt.roomTypeId(),
                        rt.roomTypeName(),
                        rt.maxOccupancy(),
                        rt.maxAdults(),
                        rt.maxChildren(),
                        rt.availableQuantity(),
                        rt.isAvailable(),
                        rt.pricePerNight(),
                        rt.totalEstimatedPrice(),
                        rt.currency()))
            .toList();

    HotelAvailabilityResponse response =
        new HotelAvailabilityResponse(
            availability.hotelId(),
            availability.checkInDate(),
            availability.checkOutDate(),
            availability.totalNights(),
            roomTypeDtos);

    return ResponseEntity.ok(ApiResponse.success("Thành công.", response));
  }
}
