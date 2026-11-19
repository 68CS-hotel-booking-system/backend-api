package com.team.hotelbooking.identity.application.usecase;

import com.team.hotelbooking.identity.application.command.ResendUserVerificationEmailCommand;
import com.team.hotelbooking.identity.application.result.ResendUserVerificationEmailResult;

public interface ResendUserVerificationEmailUseCase {

  ResendUserVerificationEmailResult execute(ResendUserVerificationEmailCommand command);
}
