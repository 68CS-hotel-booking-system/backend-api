// ReorderHotelImageAlbumsUseCase.java
package com.team.hotelbooking.media.application.usecase;

import com.team.hotelbooking.media.application.command.ReorderHotelImageAlbumsCommand;
import com.team.hotelbooking.media.application.result.HotelImageAlbumResult;
import java.util.List;

public interface ReorderHotelImageAlbumsUseCase {

  List<HotelImageAlbumResult> execute(ReorderHotelImageAlbumsCommand command);
}
