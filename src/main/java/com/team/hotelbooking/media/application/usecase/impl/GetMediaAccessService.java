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
import com.team.hotelbooking.media.domain.repository.RoomTypeImageRepository;
import com.team.hotelbooking.media.domain.repository.UserAvatarRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public final class GetMediaAccessService implements GetMediaAccessUseCase {

  private final MediaRepository media;
  private final HotelImageRepository images;
  private final RoomTypeImageRepository roomImages;
  private final UserAvatarRepository avatars;
  private final HotelImageAccess hotels;
  private final ObjectStorage storage;
  private final Clock clock;
  private final Duration ttl;

  public GetMediaAccessService(
      MediaRepository media,
      HotelImageRepository images,
      RoomTypeImageRepository roomImages,
      UserAvatarRepository avatars,
      HotelImageAccess hotels,
      ObjectStorage storage,
      Clock clock,
      Duration ttl) {

    this.media = media;
    this.images = images;
    this.roomImages = roomImages;
    this.avatars = avatars;
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

    if (image.status() != MediaObject.Status.READY) {
      throw new MediaException(MediaErrorCode.RESOURCE_NOT_FOUND);
    }
    if (image.purpose() == MediaObject.Purpose.USER_IMAGE) {
      requireAvatarOwner(query, image);
    } else if (image.purpose() == MediaObject.Purpose.HOTEL_IMAGE && image.hotelId() != null) {
      requireHotelImageAccess(query, image);
    } else {
      throw new MediaException(MediaErrorCode.RESOURCE_NOT_FOUND);
    }

    Instant expiresAt = clock.instant().plus(ttl);

    return new MediaAccessResult(
        image.id(), storage.createDownloadUrl(image.objectKey(), ttl).toString(), expiresAt);
  }

  private void requireAvatarOwner(MediaAccessQuery query, MediaObject image) {
    if (!image.createdByUserId().equals(query.actorId())
        || avatars.findMediaId(query.actorId()).filter(image.id()::equals).isEmpty()) {
      throw new MediaException(MediaErrorCode.RESOURCE_NOT_FOUND);
    }
  }

  private void requireHotelImageAccess(MediaAccessQuery query, MediaObject image) {
    if (images.isLinked(image.hotelId(), image.id()) && hotels.isPublished(image.hotelId())) {
      return;
    }
    if (roomImages.findLinkedRoomTypes(image.hotelId(), image.id()).stream()
        .anyMatch(roomTypeId -> hotels.isRoomTypePublished(image.hotelId(), roomTypeId))) {
      return;
    }
    if (query.actorId() == null) {
      throw new MediaException(MediaErrorCode.UNAUTHORIZED);
    }
    hotels.requireManage(query.actorId(), image.hotelId());
  }
}
