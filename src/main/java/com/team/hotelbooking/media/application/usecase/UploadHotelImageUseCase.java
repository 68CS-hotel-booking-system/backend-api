// UploadHotelImageUseCase.java
package com.team.hotelbooking.media.application.usecase;

import com.team.hotelbooking.media.application.command.UploadHotelImageCommand;
import com.team.hotelbooking.media.application.result.MediaResult;

public interface UploadHotelImageUseCase {

  MediaResult execute(UploadHotelImageCommand command);
}
