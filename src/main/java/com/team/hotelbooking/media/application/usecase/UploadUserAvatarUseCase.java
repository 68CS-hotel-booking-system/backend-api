package com.team.hotelbooking.media.application.usecase;

import com.team.hotelbooking.media.application.command.UploadUserAvatarCommand;
import com.team.hotelbooking.media.application.result.MediaResult;

public interface UploadUserAvatarUseCase {
  MediaResult execute(UploadUserAvatarCommand command);
}
