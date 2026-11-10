// GetMediaAccessUseCase.java
package com.team.hotelbooking.media.application.usecase;

import com.team.hotelbooking.media.application.command.MediaAccessQuery;
import com.team.hotelbooking.media.application.result.MediaAccessResult;

public interface GetMediaAccessUseCase {

  MediaAccessResult execute(MediaAccessQuery query);
}
