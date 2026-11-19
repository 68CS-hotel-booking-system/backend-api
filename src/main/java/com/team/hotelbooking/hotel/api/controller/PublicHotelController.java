package com.team.hotelbooking.hotel.api.controller;

import com.team.hotelbooking.hotel.api.dto.response.GetPublicHotelResponse;
import com.team.hotelbooking.hotel.api.dto.response.ListPublicHotelsResponse;
import com.team.hotelbooking.hotel.api.dto.response.ListPublicRoomTypesResponse;
import com.team.hotelbooking.hotel.application.command.ListPublicHotelsQuery;
import com.team.hotelbooking.hotel.application.result.GetPublicHotelResult;
import com.team.hotelbooking.hotel.application.result.ListPublicHotelsResult;
import com.team.hotelbooking.hotel.application.result.ListPublicRoomTypesResult;
import com.team.hotelbooking.hotel.application.usecase.GetPublicHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListPublicHotelsUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListPublicRoomTypesUseCase;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import com.team.hotelbooking.shared.web.ApiResponse;
import com.team.hotelbooking.shared.web.PageResponse;
import com.team.hotelbooking.shared.web.PaginationSupport;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hotels")
@RequiredArgsConstructor
public class PublicHotelController {

  private final GetPublicHotelUseCase getPublicHotelUseCase;
  private final ListPublicRoomTypesUseCase listPublicRoomTypesUseCase;
  private final ListPublicHotelsUseCase listPublicHotelsUseCase;
  private final PaginationSupport paginationSupport;

  @GetMapping("/{hotelId}")
  public ResponseEntity<ApiResponse<GetPublicHotelResponse>> getHotel(
      @PathVariable("hotelId") UUID hotelId) {
    GetPublicHotelResult result = getPublicHotelUseCase.execute(hotelId);

    GetPublicHotelResponse response = GetPublicHotelResponse.from(result);

    return ResponseEntity.ok()
        .body(ApiResponse.success("Lấy thông tin khách sạn thành công.", response));
  }

  @GetMapping("/{hotelId}/room-types")
  public ResponseEntity<ApiResponse<List<ListPublicRoomTypesResponse>>> listRoomTypes(
      @PathVariable("hotelId") UUID hotelId) {
    List<ListPublicRoomTypesResult> result = listPublicRoomTypesUseCase.execute(hotelId);

    List<ListPublicRoomTypesResponse> response = ListPublicRoomTypesResponse.from(result);

    return ResponseEntity.ok()
        .body(ApiResponse.success("Lấy danh sách loại phòng thành công.", response));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<ListPublicHotelsResponse>>> listHotels(
      @RequestParam(name = "cityId", required = false) UUID cityId,
      @RequestParam(name = "keyword", required = false) String keyword,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size) {
    PageQuery pagination = paginationSupport.resolve(page, size);
    ListPublicHotelsQuery query = new ListPublicHotelsQuery(cityId, keyword, pagination);
    PageResult<ListPublicHotelsResult> result = listPublicHotelsUseCase.execute(query);
    PageResponse<ListPublicHotelsResponse> response = ListPublicHotelsResponse.from(result);
    return ResponseEntity.ok()
        .body(ApiResponse.success("Lấy danh sách khách sạn thành công.", response));
  }
}
