package com.team.hotelbooking.media.application.usecase.impl;

import com.team.hotelbooking.media.application.command.HotelImagesQuery;
import com.team.hotelbooking.media.application.port.HotelImageAccess;
import com.team.hotelbooking.media.application.result.HotelImageResult;
import com.team.hotelbooking.media.application.usecase.GetHotelImagesUseCase;
import com.team.hotelbooking.media.domain.repository.HotelImageRepository;
import java.util.List;

public final class GetHotelImagesService implements GetHotelImagesUseCase {

  private final HotelImageAccess hotels;
  private final HotelImageRepository images;

  public GetHotelImagesService(HotelImageAccess hotels, HotelImageRepository images) {

    this.hotels = hotels;
    this.images = images;
  }

  @Override
  public List<HotelImageResult> execute(HotelImagesQuery query) {
    hotels.requireManage(query.actorId(), query.hotelId());

    return images.findByHotelId(query.hotelId()).stream().map(HotelImageResult::from).toList();
  }
}
