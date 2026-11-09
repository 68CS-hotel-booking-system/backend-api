package com.team.hotelbooking.media.application.usecase.impl;

import com.team.hotelbooking.media.application.command.ReplaceHotelImagesCommand;
import com.team.hotelbooking.media.application.port.HotelImageAccess;
import com.team.hotelbooking.media.application.result.HotelImageResult;
import com.team.hotelbooking.media.application.usecase.ReplaceHotelImagesUseCase;
import com.team.hotelbooking.media.domain.exception.MediaErrorCode;
import com.team.hotelbooking.media.domain.exception.MediaException;
import com.team.hotelbooking.media.domain.model.HotelImage;
import com.team.hotelbooking.media.domain.model.MediaObject;
import com.team.hotelbooking.media.domain.repository.HotelImageRepository;
import java.time.Clock;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ReplaceHotelImagesService implements ReplaceHotelImagesUseCase {

  private final HotelImageAccess hotels;
  private final HotelImageRepository images;
  private final Clock clock;

  public ReplaceHotelImagesService(
      HotelImageAccess hotels, HotelImageRepository images, Clock clock) {

    this.hotels = hotels;
    this.images = images;
    this.clock = clock;
  }

  @Override
  public List<HotelImageResult> execute(ReplaceHotelImagesCommand command) {

    Set<UUID> ids = new HashSet<>();
    Set<Integer> orders = new HashSet<>();
    int covers = 0;

    for (var image : command.images()) {
      if (image.mediaId() == null
          || image.sortOrder() < 0
          || !ids.add(image.mediaId())
          || !orders.add(image.sortOrder())) {
        throw new MediaException(
            MediaErrorCode.VALIDATION_FAILED, "Ảnh và thứ tự không được trùng; thứ tự phải từ 0.");
      }

      if (image.cover()) {
        covers++;
      }
    }

    if (covers > 1) {
      throw new MediaException(
          MediaErrorCode.VALIDATION_FAILED, "Mỗi khách sạn có tối đa một ảnh bìa.");
    }

    hotels.lockForUpdate(command.actorId(), command.hotelId(), command.expectedVersion());

    List<MediaObject> selected = images.lockMedia(ids.stream().sorted().toList());

    if (selected.size() != ids.size()) {
      throw new MediaException(MediaErrorCode.RESOURCE_NOT_FOUND);
    }

    for (MediaObject media : selected) {
      if (media.purpose() != MediaObject.Purpose.HOTEL_IMAGE
          || !command.hotelId().equals(media.hotelId())) {
        throw new MediaException(MediaErrorCode.ACCESS_DENIED);
      }

      if (media.status() != MediaObject.Status.READY) {
        throw new MediaException(MediaErrorCode.MEDIA_NOT_READY);
      }
    }

    List<HotelImage> replacement =
        command.images().stream()
            .map(image -> new HotelImage(image.mediaId(), image.sortOrder(), image.cover()))
            .sorted(Comparator.comparingInt(HotelImage::sortOrder))
            .toList();

    images.replace(command.hotelId(), replacement, clock.instant());

    return replacement.stream().map(HotelImageResult::from).toList();
  }
}
