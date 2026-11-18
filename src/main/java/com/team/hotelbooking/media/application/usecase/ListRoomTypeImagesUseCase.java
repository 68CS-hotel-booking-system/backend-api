package com.team.hotelbooking.media.application.usecase;

import com.team.hotelbooking.media.application.command.RoomTypeImagesQuery;
import com.team.hotelbooking.media.application.result.RoomTypeImageResult;
import java.util.List;

public interface ListRoomTypeImagesUseCase {
  List<RoomTypeImageResult> execute(RoomTypeImagesQuery query);
}
