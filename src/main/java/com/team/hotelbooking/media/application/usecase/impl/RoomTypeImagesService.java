package com.team.hotelbooking.media.application.usecase.impl;

import com.team.hotelbooking.media.application.command.AttachRoomTypeImageCommand;
import com.team.hotelbooking.media.application.command.RoomTypeImagesQuery;
import com.team.hotelbooking.media.application.port.HotelImageAccess;
import com.team.hotelbooking.media.application.result.RoomTypeImageResult;
import com.team.hotelbooking.media.application.usecase.AttachRoomTypeImageUseCase;
import com.team.hotelbooking.media.application.usecase.ListRoomTypeImagesUseCase;
import com.team.hotelbooking.media.domain.exception.MediaErrorCode;
import com.team.hotelbooking.media.domain.exception.MediaException;
import com.team.hotelbooking.media.domain.model.MediaObject;
import com.team.hotelbooking.media.domain.model.RoomTypeImage;
import com.team.hotelbooking.media.domain.repository.HotelImageRepository;
import com.team.hotelbooking.media.domain.repository.RoomTypeImageRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

public final class RoomTypeImagesService
    implements AttachRoomTypeImageUseCase, ListRoomTypeImagesUseCase {
  private final HotelImageAccess hotels;
  private final HotelImageRepository hotelImages;
  private final RoomTypeImageRepository images;
  private final Clock clock;

  public RoomTypeImagesService(
      HotelImageAccess hotels,
      HotelImageRepository hotelImages,
      RoomTypeImageRepository images,
      Clock clock) {
    this.hotels = hotels;
    this.hotelImages = hotelImages;
    this.images = images;
    this.clock = clock;
  }

  @Override
  public RoomTypeImageResult execute(AttachRoomTypeImageCommand command) {
    hotels.lockRoomTypeForImageUpdate(command.actorId(), command.hotelId(), command.roomTypeId());
    var media =
        hotelImages.lockMedia(List.of(command.mediaId())).stream()
            .filter(
                image ->
                    command.hotelId().equals(image.hotelId())
                        && image.purpose() == MediaObject.Purpose.HOTEL_IMAGE)
            .findFirst()
            .orElseThrow(() -> new MediaException(MediaErrorCode.RESOURCE_NOT_FOUND));
    if (media.status() != MediaObject.Status.READY) {
      throw new MediaException(MediaErrorCode.MEDIA_NOT_READY);
    }

    return images
        .findLink(command.hotelId(), command.roomTypeId(), media.id())
        .map(RoomTypeImageResult::from)
        .orElseGet(
            () -> {
              var link =
                  new RoomTypeImage(
                      UUID.randomUUID(),
                      command.hotelId(),
                      command.roomTypeId(),
                      media.id(),
                      clock.instant());
              images.add(link);
              return RoomTypeImageResult.from(link);
            });
  }

  @Override
  public List<RoomTypeImageResult> execute(RoomTypeImagesQuery query) {
    boolean visible;
    if (query.actorId() == null) {
      visible = hotels.isRoomTypePublished(query.hotelId(), query.roomTypeId());
    } else {
      hotels.requireManage(query.actorId(), query.hotelId());
      visible = hotels.roomTypeExists(query.hotelId(), query.roomTypeId());
    }
    if (!visible) {
      throw new MediaException(MediaErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy hạng phòng.");
    }
    return images.findReadyImages(query.hotelId(), query.roomTypeId()).stream()
        .map(RoomTypeImageResult::from)
        .toList();
  }
}
