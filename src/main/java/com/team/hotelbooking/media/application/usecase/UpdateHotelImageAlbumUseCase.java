// UpdateHotelImageAlbumUseCase.java
package com.team.hotelbooking.media.application.usecase;

import com.team.hotelbooking.media.application.command.UpdateHotelImageAlbumCommand;
import com.team.hotelbooking.media.application.result.HotelImageAlbumResult;

public interface UpdateHotelImageAlbumUseCase {

  HotelImageAlbumResult execute(UpdateHotelImageAlbumCommand command);
}
