package com.team.hotelbooking.media.api.controller;

import com.team.hotelbooking.media.api.dto.request.ReplaceHotelImagesRequest;
import com.team.hotelbooking.media.api.dto.response.HotelImageResponse;
import com.team.hotelbooking.media.application.command.HotelImagesQuery;
import com.team.hotelbooking.media.application.command.ReplaceHotelImagesCommand;
import com.team.hotelbooking.media.application.usecase.GetHotelImagesUseCase;
import com.team.hotelbooking.media.application.usecase.ReplaceHotelImagesUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/management/hotels/{hotelId}/images")
@PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER')")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.r2.enabled", havingValue = "true")
public class ManagementHotelImageController {

  private final GetHotelImagesUseCase getImagesUseCase;
  private final ReplaceHotelImagesUseCase replaceImagesUseCase;

  @GetMapping
  public ResponseEntity<ApiResponse<List<HotelImageResponse>>> list(
      @PathVariable UUID hotelId, Principal principal) {

    var result =
        getImagesUseCase.execute(
            new HotelImagesQuery(UUID.fromString(principal.getName()), hotelId));

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Danh sách liên kết ảnh quản lý thành công.",
                result.stream().map(HotelImageResponse::from).toList()));
  }

  @PutMapping
  public ResponseEntity<ApiResponse<List<HotelImageResponse>>> replace(
      @PathVariable UUID hotelId,
      @Valid @RequestBody ReplaceHotelImagesRequest request,
      Principal principal) {

    var images =
        request.images().stream()
            .map(
                image ->
                    new ReplaceHotelImagesCommand.Image(
                        image.mediaId(), image.albumId(), image.sortOrder(), image.cover()))
            .toList();

    var result =
        replaceImagesUseCase.execute(
            new ReplaceHotelImagesCommand(
                UUID.fromString(principal.getName()), hotelId, images, request.expectedVersion()));

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Thay bộ ảnh, thứ tự và tối đa một cover thành công.",
                result.stream().map(HotelImageResponse::from).toList()));
  }
}
