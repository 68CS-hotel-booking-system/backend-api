package com.team.hotelbooking.media.api.controller;

import com.team.hotelbooking.media.api.dto.request.UploadHotelImageRequest;
import com.team.hotelbooking.media.api.dto.response.MediaAccessResponse;
import com.team.hotelbooking.media.api.dto.response.MediaResponse;
import com.team.hotelbooking.media.api.dto.response.UserAvatarResponse;
import com.team.hotelbooking.media.application.command.MediaAccessQuery;
import com.team.hotelbooking.media.application.command.UploadHotelImageCommand;
import com.team.hotelbooking.media.application.command.UploadUserAvatarCommand;
import com.team.hotelbooking.media.application.usecase.GetMediaAccessUseCase;
import com.team.hotelbooking.media.application.usecase.GetUserAvatarUseCase;
import com.team.hotelbooking.media.application.usecase.UploadHotelImageUseCase;
import com.team.hotelbooking.media.application.usecase.UploadUserAvatarUseCase;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@ConditionalOnProperty(name = "storage.r2.enabled", havingValue = "true")
public class MediaController {

  private final UploadHotelImageUseCase uploadUseCase;
  private final GetMediaAccessUseCase accessUseCase;
  private final UploadUserAvatarUseCase uploadUserAvatarUseCase;
  private final GetUserAvatarUseCase getUserAvatarUseCase;
  private final int maxBytes;

  public MediaController(
      UploadHotelImageUseCase uploadUseCase,
      GetMediaAccessUseCase accessUseCase,
      UploadUserAvatarUseCase uploadUserAvatarUseCase,
      GetUserAvatarUseCase getUserAvatarUseCase,
      @Value("${media.upload.max-bytes:5242880}") int maxBytes) {

    if (maxBytes <= 0 || maxBytes == Integer.MAX_VALUE) {
      throw new IllegalArgumentException("media.upload.max-bytes không hợp lệ.");
    }

    this.uploadUseCase = uploadUseCase;
    this.accessUseCase = accessUseCase;
    this.uploadUserAvatarUseCase = uploadUserAvatarUseCase;
    this.getUserAvatarUseCase = getUserAvatarUseCase;
    this.maxBytes = maxBytes;
  }

  @PostMapping(path = "/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('ADMIN', 'HOTEL_OWNER')")
  public ResponseEntity<ApiResponse<MediaResponse>> upload(
      @Valid @ModelAttribute UploadHotelImageRequest request, Principal principal)
      throws IOException {

    var result =
        uploadUseCase.execute(
            new UploadHotelImageCommand(
                UUID.fromString(principal.getName()),
                request.hotelId(),
                request.file().getOriginalFilename(),
                readImage(request.file())));
    return ResponseEntity.status(HttpStatus.CREATED)
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success("Tải và kiểm tra ảnh thành công.", MediaResponse.from(result)));
  }

  @PutMapping(path = "/users/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<UserAvatarResponse>> uploadAvatar(
      @RequestParam MultipartFile file, Principal principal) throws IOException {
    var result =
        uploadUserAvatarUseCase.execute(
            new UploadUserAvatarCommand(
                UUID.fromString(principal.getName()), file.getOriginalFilename(), readImage(file)));
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            ApiResponse.success("Đã cập nhật ảnh đại diện.", new UserAvatarResponse(result.id())));
  }

  @GetMapping("/users/me/avatar")
  public ResponseEntity<ApiResponse<UserAvatarResponse>> getAvatar(Principal principal) {
    var mediaId = getUserAvatarUseCase.execute(UUID.fromString(principal.getName())).orElse(null);
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success(new UserAvatarResponse(mediaId)));
  }

  private byte[] readImage(MultipartFile file) throws IOException {
    if (file.isEmpty()) {
      throw new MediaException(MediaErrorCode.VALIDATION_FAILED, "Vui lòng chọn ảnh.");
    }

    if (file.getSize() > maxBytes) {
      throw new MediaException(MediaErrorCode.PAYLOAD_TOO_LARGE);
    }

    byte[] content;

    try (InputStream input = file.getInputStream()) {
      content = input.readNBytes(maxBytes + 1);
    }

    if (content.length > maxBytes) {
      throw new MediaException(MediaErrorCode.PAYLOAD_TOO_LARGE);
    }

    return content;
  }

  @GetMapping("/media/{mediaId}/access")
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
