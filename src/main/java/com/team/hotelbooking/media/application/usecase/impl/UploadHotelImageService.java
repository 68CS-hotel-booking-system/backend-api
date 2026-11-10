package com.team.hotelbooking.media.application.usecase.impl;

import com.team.hotelbooking.media.application.command.UploadHotelImageCommand;
import com.team.hotelbooking.media.application.port.HotelImageAccess;
import com.team.hotelbooking.media.application.port.ImageInspector;
import com.team.hotelbooking.media.application.port.ObjectStorage;
import com.team.hotelbooking.media.application.result.MediaResult;
import com.team.hotelbooking.media.application.usecase.UploadHotelImageUseCase;
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

public final class UploadHotelImageService implements UploadHotelImageUseCase {

  private final MediaRepository media;
  private final HotelImageAccess hotels;
  private final ImageInspector inspector;
  private final ObjectStorage storage;
  private final Clock clock;
  private final String bucket;
  private final long maxBytes;

  public UploadHotelImageService(
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

    hotels.requireManage(command.actorId(), command.hotelId());

    byte[] content = command.content();

    if (content == null || content.length == 0) {
      throw new MediaException(MediaErrorCode.VALIDATION_FAILED, "Vui lòng chọn ảnh.");
    }

    if (content.length > maxBytes) {
      throw new MediaException(MediaErrorCode.PAYLOAD_TOO_LARGE);
    }

    ImageInspector.ImageInfo info = inspector.inspect(content);

    UUID id = UUID.randomUUID();
    Instant now = clock.instant();

    String key = "hotels/" + command.hotelId() + "/images/" + id + "." + info.extension();

    MediaObject pending =
        new MediaObject(
            id,
            "R2",
            bucket,
            key,
            filename(command.originalFilename()),
            info.contentType(),
            content.length,
            sha256(content),
            MediaObject.Purpose.HOTEL_IMAGE,
            command.hotelId(),
            MediaObject.Status.PENDING,
            command.actorId(),
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
