package com.team.hotelbooking.hotel.api.controller;

import com.team.hotelbooking.hotel.api.dto.response.GetPublicHotelResponse;
import com.team.hotelbooking.hotel.api.dto.response.ListPublicRoomTypesResponse;
import com.team.hotelbooking.hotel.application.result.GetPublicHotelResult;
import com.team.hotelbooking.hotel.application.result.ListPublicRoomTypesResult;
import com.team.hotelbooking.hotel.application.usecase.GetPublicHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListPublicRoomTypesUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hotels")
@RequiredArgsConstructor
public class PublicHotelController {

  private final GetPublicHotelUseCase getPublicHotelUseCase;
  private final ListPublicRoomTypesUseCase listPublicRoomTypesUseCase;

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
}
