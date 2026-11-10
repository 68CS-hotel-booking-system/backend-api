// CreateHotelImageAlbumUseCase.java
package com.team.hotelbooking.media.application.usecase;

import com.team.hotelbooking.media.application.command.CreateHotelImageAlbumCommand;
import com.team.hotelbooking.media.application.result.HotelImageAlbumResult;

public interface CreateHotelImageAlbumUseCase {

  HotelImageAlbumResult execute(CreateHotelImageAlbumCommand command);
}
