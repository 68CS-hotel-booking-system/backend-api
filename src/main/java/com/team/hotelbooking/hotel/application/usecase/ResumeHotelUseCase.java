package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.command.ResumeHotelCommand;
import com.team.hotelbooking.hotel.application.result.ManagementHotelResult;

public interface ResumeHotelUseCase {

  ManagementHotelResult execute(ResumeHotelCommand command);
}
