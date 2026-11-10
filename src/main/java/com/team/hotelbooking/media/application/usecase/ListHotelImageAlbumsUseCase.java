// ListHotelImageAlbumsUseCase.java
package com.team.hotelbooking.media.application.usecase;

import com.team.hotelbooking.media.application.command.ListHotelImageAlbumsQuery;
import com.team.hotelbooking.media.application.result.HotelImageAlbumResult;
import java.util.List;

public interface ListHotelImageAlbumsUseCase {

  List<HotelImageAlbumResult> execute(ListHotelImageAlbumsQuery query);
}
