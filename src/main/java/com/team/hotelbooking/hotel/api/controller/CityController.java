package com.team.hotelbooking.hotel.api.controller;

import com.team.hotelbooking.hotel.api.dto.response.ListCitiesResponse;
import com.team.hotelbooking.hotel.application.result.ListCitiesResult;
import com.team.hotelbooking.hotel.application.usecase.ListCitiesUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cities")
@RequiredArgsConstructor
public class CityController {

  private final ListCitiesUseCase listCitiesUseCase;

  @GetMapping
  public ResponseEntity<ApiResponse<List<ListCitiesResponse>>> listCities() {
    List<ListCitiesResult> result = listCitiesUseCase.execute();

    List<ListCitiesResponse> response = ListCitiesResponse.from(result);

    return ResponseEntity.ok()
        .body(ApiResponse.success("Lấy danh sách thành phố thành công.", response));
  }
}
