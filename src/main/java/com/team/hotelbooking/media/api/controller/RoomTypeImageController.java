package com.team.hotelbooking.media.api.controller;

import com.team.hotelbooking.media.api.dto.request.AttachRoomTypeImageRequest;
import com.team.hotelbooking.media.api.dto.response.RoomTypeImageResponse;
import com.team.hotelbooking.media.application.command.AttachRoomTypeImageCommand;
import com.team.hotelbooking.media.application.command.RoomTypeImagesQuery;
import com.team.hotelbooking.media.application.usecase.AttachRoomTypeImageUseCase;
import com.team.hotelbooking.media.application.usecase.ListRoomTypeImagesUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.r2.enabled", havingValue = "true")
public class RoomTypeImageController {
  private static final String MANAGEMENT =
      "/management/hotels/{hotelId}/room-types/{roomTypeId}/images";
  private final AttachRoomTypeImageUseCase attach;
  private final ListRoomTypeImagesUseCase list;

  @PostMapping(MANAGEMENT)
  @PreAuthorize("hasRole('HOTEL_OWNER')")
  public ApiResponse<RoomTypeImageResponse> attach(
      @PathVariable UUID hotelId,
      @PathVariable UUID roomTypeId,
      @Valid @RequestBody AttachRoomTypeImageRequest request,
      Principal principal) {
    var command =
        new AttachRoomTypeImageCommand(
            UUID.fromString(principal.getName()), hotelId, roomTypeId, request.mediaId());
    return ApiResponse.success(
        "Đã gắn ảnh vào hạng phòng.", RoomTypeImageResponse.from(attach.execute(command)));
  }

  @GetMapping(MANAGEMENT)
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER')")
  public ApiResponse<List<RoomTypeImageResponse>> listManagement(
      @PathVariable UUID hotelId, @PathVariable UUID roomTypeId, Principal principal) {
    return listImages(
        new RoomTypeImagesQuery(hotelId, roomTypeId, UUID.fromString(principal.getName())));
  }

  @GetMapping("/hotels/{hotelId}/room-types/{roomTypeId}/images")
  public ApiResponse<List<RoomTypeImageResponse>> listPublic(
      @PathVariable UUID hotelId, @PathVariable UUID roomTypeId) {
    return listImages(new RoomTypeImagesQuery(hotelId, roomTypeId, null));
  }

  private ApiResponse<List<RoomTypeImageResponse>> listImages(RoomTypeImagesQuery query) {
    return ApiResponse.success(
        list.execute(query).stream().map(RoomTypeImageResponse::from).toList());
  }
}
