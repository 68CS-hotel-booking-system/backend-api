package com.team.hotelbooking.media.api.controller;

import com.team.hotelbooking.media.api.dto.response.PublicHotelGalleryResponse;
import com.team.hotelbooking.media.application.usecase.GetPublicHotelGalleryUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hotels/{hotelId}/gallery")
public class PublicHotelGalleryController {
  private final GetPublicHotelGalleryUseCase useCase;

  public PublicHotelGalleryController(GetPublicHotelGalleryUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PublicHotelGalleryResponse>> get(
      @PathVariable("hotelId") UUID hotelId) {
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Lấy ảnh khách sạn thành công.",
                PublicHotelGalleryResponse.from(useCase.execute(hotelId))));
  }
}
