package com.team.hotelbooking.media.application.usecase;

import com.team.hotelbooking.media.application.result.PublicHotelGalleryResult;
import java.util.UUID;

public interface GetPublicHotelGalleryUseCase {
  PublicHotelGalleryResult execute(UUID hotelId);
}
