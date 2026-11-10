package com.team.hotelbooking.media.domain.model;

import java.time.Instant;
import java.util.UUID;

public record MediaObject(
    UUID id,
    String storageProvider,
    String bucketName,
    String objectKey,
    String originalFilename,
    String contentType,
    long sizeBytes,
    String checksumSha256,
    Purpose purpose,
    UUID hotelId,
    Status status,
    UUID createdByUserId,
    Instant createdAt,
    Instant deletedAt) {

  public enum Purpose {
    USER_IMAGE,
    HOTEL_IMAGE,
    PRIVATE_EVIDENCE
  }

  public enum Status {
    PENDING,
    READY,
    DELETED
  }

  public MediaObject ready() {
    return new MediaObject(
        id,
        storageProvider,
        bucketName,
        objectKey,
        originalFilename,
        contentType,
        sizeBytes,
        checksumSha256,
        purpose,
        hotelId,
        Status.READY,
        createdByUserId,
        createdAt,
        null);
  }
}
