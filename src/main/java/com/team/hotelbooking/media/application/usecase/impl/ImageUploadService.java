package com.team.hotelbooking.media.application.usecase.impl;

import com.team.hotelbooking.media.application.command.UploadHotelImageCommand;
import com.team.hotelbooking.media.application.command.UploadUserAvatarCommand;
import com.team.hotelbooking.media.application.port.HotelImageAccess;
import com.team.hotelbooking.media.application.port.ImageInspector;
import com.team.hotelbooking.media.application.port.ObjectStorage;
import com.team.hotelbooking.media.application.result.MediaResult;
import com.team.hotelbooking.media.application.usecase.UploadHotelImageUseCase;
import com.team.hotelbooking.media.application.usecase.UploadUserAvatarUseCase;
import com.team.hotelbooking.media.domain.exception.MediaErrorCode;
import com.team.hotelbooking.media.domain.exception.MediaException;
import com.team.hotelbooking.media.domain.model.MediaObject;
import com.team.hotelbooking.media.domain.repository.MediaRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

public final class ImageUploadService implements UploadHotelImageUseCase, UploadUserAvatarUseCase {

  private final MediaRepository media;
  private final HotelImageAccess hotels;
  private final ImageInspector inspector;
  private final ObjectStorage storage;
  private final Clock clock;
  private final String bucket;
  private final long maxBytes;

  public ImageUploadService(
      MediaRepository media,
      HotelImageAccess hotels,
      ImageInspector inspector,
      ObjectStorage storage,
      Clock clock,
      String bucket,
      long maxBytes) {

    this.media = media;
    this.hotels = hotels;
    this.inspector = inspector;
    this.storage = storage;
    this.clock = clock;
    this.bucket = bucket;
    this.maxBytes = maxBytes;
  }

  @Override
  public MediaResult execute(UploadHotelImageCommand command) {
    if (command == null || command.actorId() == null || command.hotelId() == null) {
      throw new MediaException(MediaErrorCode.VALIDATION_FAILED);
    }

    hotels.requireUpload(command.actorId(), command.hotelId());
    return upload(
        command.actorId(), command.hotelId(), command.originalFilename(), command.content());
  }

  @Override
  public MediaResult execute(UploadUserAvatarCommand command) {
    if (command == null || command.actorId() == null) {
      throw new MediaException(MediaErrorCode.UNAUTHORIZED);
    }
    return upload(command.actorId(), null, command.originalFilename(), command.content());
  }

  private MediaResult upload(UUID actorId, UUID hotelId, String originalFilename, byte[] content) {

    if (content == null || content.length == 0) {
      throw new MediaException(MediaErrorCode.VALIDATION_FAILED, "Vui lòng chọn ảnh.");
    }

    if (content.length > maxBytes) {
      throw new MediaException(MediaErrorCode.PAYLOAD_TOO_LARGE);
    }

    ImageInspector.ImageInfo info = inspector.inspect(content);

    UUID id = UUID.randomUUID();
    Instant now = clock.instant();

    boolean avatar = hotelId == null;
    String prefix = avatar ? "users/" + actorId + "/avatars/" : "hotels/" + hotelId + "/images/";
    String key = prefix + id + "." + info.extension();

    MediaObject pending =
        new MediaObject(
            id,
            "R2",
            bucket,
            key,
            filename(originalFilename),
            info.contentType(),
            content.length,
            sha256(content),
            avatar ? MediaObject.Purpose.USER_IMAGE : MediaObject.Purpose.HOTEL_IMAGE,
            hotelId,
            MediaObject.Status.PENDING,
            actorId,
            now,
            null);

    media.add(pending);

    try {
      storage.upload(key, content, info.contentType());
      media.markReady(id);

      return MediaResult.from(pending.ready());
    } catch (RuntimeException failure) {
      try {
        var current = media.findById(id);

        if (current.isPresent() && current.get().status() == MediaObject.Status.READY) {
          return MediaResult.from(current.get());
        }

        if (current.isPresent() && current.get().status() == MediaObject.Status.PENDING) {
          storage.delete(key);
          media.markDeleted(id, clock.instant());
        }
      } catch (RuntimeException cleanupFailure) {
        failure.addSuppressed(cleanupFailure);
      }

      throw failure;
    }
  }

  private static String filename(String original) {
    String name = original == null ? "upload" : original.replace('\\', '/');

    name = name.substring(name.lastIndexOf('/') + 1);
    name = name.replaceAll("\\p{Cntrl}", "").trim();

    if (name.isBlank()) {
      name = "upload";
    }

    return name.length() > 255 ? name.substring(0, 255) : name;
  }

  private static String sha256(byte[] content) {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(content);

      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 không khả dụng.", exception);
    }
  }
}
