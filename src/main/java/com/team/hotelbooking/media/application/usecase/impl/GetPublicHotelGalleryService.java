package com.team.hotelbooking.media.application.usecase.impl;

import com.team.hotelbooking.media.application.port.HotelImageAccess;
import com.team.hotelbooking.media.application.result.PublicHotelGalleryResult;
import com.team.hotelbooking.media.application.usecase.GetPublicHotelGalleryUseCase;
import com.team.hotelbooking.media.domain.exception.MediaErrorCode;
import com.team.hotelbooking.media.domain.exception.MediaException;
import com.team.hotelbooking.media.domain.repository.HotelImageAlbumRepository;
import com.team.hotelbooking.media.domain.repository.HotelImageRepository;
import java.util.Comparator;
import java.util.UUID;

public final class GetPublicHotelGalleryService implements GetPublicHotelGalleryUseCase {
  private final HotelImageAccess hotels;
  private final HotelImageRepository images;
  private final HotelImageAlbumRepository albums;

  public GetPublicHotelGalleryService(
      HotelImageAccess hotels, HotelImageRepository images, HotelImageAlbumRepository albums) {
    this.hotels = hotels;
    this.images = images;
    this.albums = albums;
  }

  @Override
  public PublicHotelGalleryResult execute(UUID hotelId) {
    if (!hotels.isPublished(hotelId)) throw new MediaException(MediaErrorCode.RESOURCE_NOT_FOUND);
    var photos =
        images.findPublicByHotelId(hotelId).stream()
            .map(
                i ->
                    new PublicHotelGalleryResult.Image(
                        i.mediaId(), i.albumId(), i.sortOrder(), i.cover()))
            .toList();
    var groups =
        albums.findByHotelId(hotelId).stream()
            .sorted(Comparator.comparingInt(a -> a.sortOrder()))
            .filter(a -> photos.stream().anyMatch(i -> a.id().equals(i.albumId())))
            .map(
                a ->
                    new PublicHotelGalleryResult.Album(
                        a.id(), a.name(), a.description(), a.sortOrder()))
            .toList();
    return new PublicHotelGalleryResult(hotelId, photos, groups);
  }
}
