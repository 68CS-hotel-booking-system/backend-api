package com.team.hotelbooking.media.application.usecase.impl;

import com.team.hotelbooking.media.application.command.MediaAccessQuery;
import com.team.hotelbooking.media.application.port.HotelImageAccess;
import com.team.hotelbooking.media.application.port.ObjectStorage;
import com.team.hotelbooking.media.application.result.MediaAccessResult;
import com.team.hotelbooking.media.application.usecase.GetMediaAccessUseCase;
import com.team.hotelbooking.media.domain.exception.MediaErrorCode;
import com.team.hotelbooking.media.domain.exception.MediaException;
import com.team.hotelbooking.media.domain.model.MediaObject;
import com.team.hotelbooking.media.domain.repository.HotelImageRepository;
import com.team.hotelbooking.media.domain.repository.MediaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public final class GetMediaAccessService implements GetMediaAccessUseCase {

  private final MediaRepository media;
  private final HotelImageRepository images;
  private final HotelImageAccess hotels;
  private final ObjectStorage storage;
  private final Clock clock;
  private final Duration ttl;

  public GetMediaAccessService(
      MediaRepository media,
      HotelImageRepository images,
      HotelImageAccess hotels,
      ObjectStorage storage,
      Clock clock,
      Duration ttl) {

    this.media = media;
    this.images = images;
    this.hotels = hotels;
    this.storage = storage;
    this.clock = clock;
    this.ttl = ttl;
  }

  @Override
  public MediaAccessResult execute(MediaAccessQuery query) {
    MediaObject image =
        media
            .findById(query.mediaId())
            .orElseThrow(() -> new MediaException(MediaErrorCode.RESOURCE_NOT_FOUND));

    if (image.status() != MediaObject.Status.READY
        || image.purpose() != MediaObject.Purpose.HOTEL_IMAGE
        || image.hotelId() == null) {
      throw new MediaException(MediaErrorCode.RESOURCE_NOT_FOUND);
    }

    boolean publicImage =
        images.isLinked(image.hotelId(), image.id()) && hotels.isPublished(image.hotelId());

    if (!publicImage) {
      if (query.actorId() == null) {
        throw new MediaException(MediaErrorCode.UNAUTHORIZED);
      }

      hotels.requireManage(query.actorId(), image.hotelId());
    }

    Instant expiresAt = clock.instant().plus(ttl);

    return new MediaAccessResult(
        image.id(), storage.createDownloadUrl(image.objectKey(), ttl).toString(), expiresAt);
  }
}
