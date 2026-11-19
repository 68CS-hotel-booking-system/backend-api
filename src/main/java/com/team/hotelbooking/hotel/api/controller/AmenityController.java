package com.team.hotelbooking.hotel.api.controller;

import com.team.hotelbooking.hotel.api.dto.response.ListAmenitiesResponse;
import com.team.hotelbooking.hotel.application.result.ListAmenitiesResult;
import com.team.hotelbooking.hotel.application.usecase.ListAmenitiesUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/amenities")
@RequiredArgsConstructor
public class AmenityController {

  private final ListAmenitiesUseCase listAmenitiesUseCase;

  @GetMapping
  public ResponseEntity<ApiResponse<List<ListAmenitiesResponse>>> listAmenities(
      @RequestParam(name = "category", required = false) String category) {
    List<ListAmenitiesResult> result = listAmenitiesUseCase.execute(category);

    List<ListAmenitiesResponse> response = ListAmenitiesResponse.from(result);

    return ResponseEntity.ok()
        .body(ApiResponse.success("Lấy danh sách tiện ích thành công.", response));
  }
}
