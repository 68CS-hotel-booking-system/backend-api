package com.team.hotelbooking.inventory.api.controller;

import com.team.hotelbooking.inventory.api.dto.request.SearchHotelsRequest;
import com.team.hotelbooking.inventory.api.dto.response.HotelSearchItemResponse;
import com.team.hotelbooking.inventory.application.result.HotelSearchItemResult;
import com.team.hotelbooking.inventory.application.usecase.SearchHotelsUseCase;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.web.ApiResponse;
import com.team.hotelbooking.shared.web.PageResponse;
import com.team.hotelbooking.shared.web.PaginationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// GET /api/v1/hotels (danh sách của module hotel) giữ nguyên; tìm kiếm có ngày/giá/tồn ở đây.
@RestController
@RequestMapping("/api/v1/hotels")
@RequiredArgsConstructor
@Tag(name = "Inventory - Search", description = "Tìm kiếm khách sạn công khai")
public class HotelSearchController {

  private final SearchHotelsUseCase searchHotelsUseCase;
  private final PaginationSupport paginationSupport;

  @Operation(
      summary = "Tìm kiếm khách sạn đang bán",
      description =
          "Chỉ khách sạn ACTIVE có ít nhất một loại phòng đủ sức chứa (guests) và có phòng đang"
              + " phục vụ. Có checkIn/checkOut: loại phòng phải còn phòng MỌI đêm và có giá hợp lệ"
              + " MỌI đêm (room_rates, không có thì base_price; giá ≤ 0 bị loại); minNightlyPrice là"
              + " giá trung bình một đêm của loại phòng rẻ nhất, totalPrice là tổng một phòng cả kỳ."
              + " Không có ngày: chỉ lọc tĩnh, minNightlyPrice là base_price nhỏ nhất (> 0),"
              + " totalPrice null. coverImage chỉ có mediaId (null khi chưa có ảnh bìa): dùng"
              + " mediaId gọi GET /api/v1/media/{mediaId}/access để lấy URL, URL hết hạn sau TTL.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Tìm kiếm thành công (có thể rỗng)"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Tham số sai: ngày, khoảng giá, số sao, sort, phân trang",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  })
  @GetMapping("/search")
  public ResponseEntity<ApiResponse<PageResponse<HotelSearchItemResponse>>> searchHotels(
      @ParameterObject @Valid SearchHotelsRequest request) {
    PageQuery pagination = paginationSupport.resolve(request.page(), request.size());

    PageResult<HotelSearchItemResult> result =
        searchHotelsUseCase.execute(request.toQuery(pagination));

    PageResponse<HotelSearchItemResponse> response = HotelSearchItemResponse.from(result);

    return ResponseEntity.ok(ApiResponse.success("Tìm kiếm khách sạn thành công.", response));
  }
}
