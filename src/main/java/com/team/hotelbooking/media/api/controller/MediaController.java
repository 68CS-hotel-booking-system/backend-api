package com.team.hotelbooking.media.api.controller;

import com.team.hotelbooking.media.api.dto.request.UploadHotelImageRequest;
import com.team.hotelbooking.media.api.dto.response.MediaAccessResponse;
import com.team.hotelbooking.media.api.dto.response.MediaResponse;
import com.team.hotelbooking.media.application.command.MediaAccessQuery;
import com.team.hotelbooking.media.application.command.UploadHotelImageCommand;
import com.team.hotelbooking.media.application.usecase.GetMediaAccessUseCase;
import com.team.hotelbooking.media.application.usecase.UploadHotelImageUseCase;
import com.team.hotelbooking.media.domain.exception.MediaErrorCode;
import com.team.hotelbooking.media.domain.exception.MediaException;
import com.team.hotelbooking.shared.web.ApiResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/media")
@ConditionalOnProperty(name = "storage.r2.enabled", havingValue = "true")
public class MediaController {

  private final UploadHotelImageUseCase uploadUseCase;
  private final GetMediaAccessUseCase accessUseCase;
  private final int maxBytes;

  public MediaController(
      UploadHotelImageUseCase uploadUseCase,
      GetMediaAccessUseCase accessUseCase,
      @Value("${media.upload.max-bytes:5242880}") int maxBytes) {

    if (maxBytes <= 0 || maxBytes == Integer.MAX_VALUE) {
      throw new IllegalArgumentException("media.upload.max-bytes không hợp lệ.");
    }

    this.uploadUseCase = uploadUseCase;
    this.accessUseCase = accessUseCase;
    this.maxBytes = maxBytes;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER')")
  public ResponseEntity<ApiResponse<MediaResponse>> upload(
      @Valid @ModelAttribute UploadHotelImageRequest request, Principal principal)
      throws IOException {

    if (request.file().isEmpty()) {
      throw new MediaException(MediaErrorCode.VALIDATION_FAILED, "Vui lòng chọn ảnh.");
    }

    if (request.file().getSize() > maxBytes) {
      throw new MediaException(MediaErrorCode.PAYLOAD_TOO_LARGE);
    }

    byte[] content;

    try (InputStream input = request.file().getInputStream()) {
      content = input.readNBytes(maxBytes + 1);
    }

    if (content.length > maxBytes) {
      throw new MediaException(MediaErrorCode.PAYLOAD_TOO_LARGE);
    }

    var result =
        uploadUseCase.execute(
            new UploadHotelImageCommand(
                UUID.fromString(principal.getName()),
                request.hotelId(),
                request.file().getOriginalFilename(),
                content));

    return ResponseEntity.status(HttpStatus.CREATED)
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Tải và kiểm tra ảnh thành công.", MediaResponse.from(result)));
  }

  @GetMapping("/{mediaId}/access")
  public ResponseEntity<ApiResponse<MediaAccessResponse>> access(
      @PathVariable UUID mediaId, Principal principal) {

    UUID actorId = principal == null ? null : UUID.fromString(principal.getName());

    var result = accessUseCase.execute(new MediaAccessQuery(actorId, mediaId));

    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success(
                "URL truy cập ảnh theo quyền thành công.", MediaAccessResponse.from(result)));
  }
}
