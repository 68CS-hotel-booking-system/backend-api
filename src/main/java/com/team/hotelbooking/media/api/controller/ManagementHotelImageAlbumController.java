package com.team.hotelbooking.media.api.controller;

import com.team.hotelbooking.media.api.dto.request.CreateHotelImageAlbumRequest;
import com.team.hotelbooking.media.api.dto.request.ReorderHotelImageAlbumsRequest;
import com.team.hotelbooking.media.api.dto.request.UpdateHotelImageAlbumRequest;
import com.team.hotelbooking.media.api.dto.response.DeleteHotelImageAlbumResponse;
import com.team.hotelbooking.media.api.dto.response.HotelImageAlbumResponse;
import com.team.hotelbooking.media.application.command.CreateHotelImageAlbumCommand;
import com.team.hotelbooking.media.application.command.DeleteHotelImageAlbumCommand;
import com.team.hotelbooking.media.application.command.ListHotelImageAlbumsQuery;
import com.team.hotelbooking.media.application.command.ReorderHotelImageAlbumsCommand;
import com.team.hotelbooking.media.application.command.UpdateHotelImageAlbumCommand;
import com.team.hotelbooking.media.application.usecase.CreateHotelImageAlbumUseCase;
import com.team.hotelbooking.media.application.usecase.DeleteHotelImageAlbumUseCase;
import com.team.hotelbooking.media.application.usecase.ListHotelImageAlbumsUseCase;
import com.team.hotelbooking.media.application.usecase.ReorderHotelImageAlbumsUseCase;
import com.team.hotelbooking.media.application.usecase.UpdateHotelImageAlbumUseCase;
import com.team.hotelbooking.shared.web.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/management/hotels/{hotelId}/image-albums")
@PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER')")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.r2.enabled", havingValue = "true")
public class ManagementHotelImageAlbumController {

  private final ListHotelImageAlbumsUseCase listUseCase;
  private final CreateHotelImageAlbumUseCase createUseCase;
  private final UpdateHotelImageAlbumUseCase updateUseCase;
  private final ReorderHotelImageAlbumsUseCase reorderUseCase;
  private final DeleteHotelImageAlbumUseCase deleteUseCase;

  @GetMapping
  public ResponseEntity<ApiResponse<List<HotelImageAlbumResponse>>> list(
      @PathVariable("hotelId") UUID hotelId, Principal principal) {

    var result = listUseCase.execute(new ListHotelImageAlbumsQuery(actorId(principal), hotelId));

    var response = result.stream().map(HotelImageAlbumResponse::from).toList();

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Lấy danh sách album ảnh thành công.", response));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<HotelImageAlbumResponse>> create(
      @PathVariable("hotelId") UUID hotelId,
      @Valid @RequestBody CreateHotelImageAlbumRequest request,
      Principal principal) {

    var result =
        createUseCase.execute(
            new CreateHotelImageAlbumCommand(
                actorId(principal),
                hotelId,
                request.name(),
                request.description(),
                request.expectedVersion()));

    return ResponseEntity.status(HttpStatus.CREATED)
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success("Tạo album ảnh thành công.", HotelImageAlbumResponse.from(result)));
  }

  @PatchMapping("/{albumId}")
  public ResponseEntity<ApiResponse<HotelImageAlbumResponse>> update(
      @PathVariable("hotelId") UUID hotelId,
      @PathVariable("albumId") UUID albumId,
      @Valid @RequestBody UpdateHotelImageAlbumRequest request,
      Principal principal) {

    var result =
        updateUseCase.execute(
            new UpdateHotelImageAlbumCommand(
                actorId(principal),
                hotelId,
                albumId,
                request.name(),
                request.description(),
                request.expectedVersion()));

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Cập nhật album ảnh thành công.", HotelImageAlbumResponse.from(result)));
  }

  @PutMapping("/order")
  public ResponseEntity<ApiResponse<List<HotelImageAlbumResponse>>> reorder(
      @PathVariable("hotelId") UUID hotelId,
      @Valid @RequestBody ReorderHotelImageAlbumsRequest request,
      Principal principal) {

    var result =
        reorderUseCase.execute(
            new ReorderHotelImageAlbumsCommand(
                actorId(principal), hotelId, request.albumIds(), request.expectedVersion()));

    var response = result.stream().map(HotelImageAlbumResponse::from).toList();

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Sắp xếp album ảnh thành công.", response));
  }

  @DeleteMapping("/{albumId}")
  public ResponseEntity<ApiResponse<DeleteHotelImageAlbumResponse>> delete(
      @PathVariable("hotelId") UUID hotelId,
      @PathVariable("albumId") UUID albumId,
      @RequestParam("expectedVersion") @Min(0) long expectedVersion,
      Principal principal) {

    var result =
        deleteUseCase.execute(
            new DeleteHotelImageAlbumCommand(
                actorId(principal), hotelId, albumId, expectedVersion));

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "Xóa album ảnh thành công.", DeleteHotelImageAlbumResponse.from(result)));
  }

  private static UUID actorId(Principal principal) {
    return UUID.fromString(principal.getName());
  }
}
