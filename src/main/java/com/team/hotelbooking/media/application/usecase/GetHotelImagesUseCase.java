// GetHotelImagesUseCase.java
package com.team.hotelbooking.media.application.usecase;

import com.team.hotelbooking.media.application.command.HotelImagesQuery;
import com.team.hotelbooking.media.application.result.HotelImageResult;
import java.util.List;

public interface GetHotelImagesUseCase {

  List<HotelImageResult> execute(HotelImagesQuery query);
}
