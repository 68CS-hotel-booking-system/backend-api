// ReplaceHotelImagesUseCase.java
package com.team.hotelbooking.media.application.usecase;

import com.team.hotelbooking.media.application.command.ReplaceHotelImagesCommand;
import com.team.hotelbooking.media.application.result.HotelImageResult;
import java.util.List;

public interface ReplaceHotelImagesUseCase {

  List<HotelImageResult> execute(ReplaceHotelImagesCommand command);
}
