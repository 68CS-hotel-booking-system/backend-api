package com.team.hotelbooking.pricing.api.controller;

import com.team.hotelbooking.pricing.api.dto.request.CalculateStayQuoteRequest;
import com.team.hotelbooking.pricing.api.dto.response.StayQuoteResponse;
import com.team.hotelbooking.pricing.application.result.StayQuoteResult;
import com.team.hotelbooking.pricing.application.usecase.CalculateStayQuoteUseCase;
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
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hotels")
@RequiredArgsConstructor
@Tag(name = "Pricing - Quote", description = "API báo giá lưu trú trước khi đặt phòng")
public class PublicQuoteController {

  private final CalculateStayQuoteUseCase calculateStayQuoteUseCase;

  @Operation(
      summary = "[PR03] Báo giá lưu trú chi tiết trước khi đặt phòng",
      description =
          "Báo giá tạm tính theo từng đêm; giờ dự kiến chỉ là ghi chú, không tính phụ thu. Giá và phòng được kiểm tra lại khi đặt.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Báo giá lưu trú thành công"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Khoảng ngày, sức chứa hoặc dữ liệu báo giá không hợp lệ",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Không tìm thấy khách sạn hoặc loại phòng",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "Loại phòng chưa có giá bán hợp lệ cho tất cả các đêm",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  @PostMapping("/{hotelId}/quote")
  public ResponseEntity<ApiResponse<StayQuoteResponse>> calculateStayQuote(
      @Parameter(description = "ID khách sạn", required = true) @PathVariable("hotelId")
          UUID hotelId,
      @Valid @RequestBody CalculateStayQuoteRequest request) {

    StayQuoteResult result = calculateStayQuoteUseCase.execute(request.toQuery(hotelId));

    StayQuoteResponse response = StayQuoteResponse.from(result);

    return ResponseEntity.ok(ApiResponse.success("Báo giá lưu trú thành công.", response));
  }
}
