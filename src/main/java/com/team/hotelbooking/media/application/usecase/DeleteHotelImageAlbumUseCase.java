// DeleteHotelImageAlbumUseCase.java
package com.team.hotelbooking.media.application.usecase;

import com.team.hotelbooking.media.application.command.DeleteHotelImageAlbumCommand;
import com.team.hotelbooking.media.application.result.DeleteHotelImageAlbumResult;

public interface DeleteHotelImageAlbumUseCase {

  DeleteHotelImageAlbumResult execute(DeleteHotelImageAlbumCommand command);
}
