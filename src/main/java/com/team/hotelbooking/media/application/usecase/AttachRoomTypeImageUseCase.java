package com.team.hotelbooking.media.application.usecase;

import com.team.hotelbooking.media.application.command.AttachRoomTypeImageCommand;
import com.team.hotelbooking.media.application.result.RoomTypeImageResult;

public interface AttachRoomTypeImageUseCase {
  RoomTypeImageResult execute(AttachRoomTypeImageCommand command);
}
