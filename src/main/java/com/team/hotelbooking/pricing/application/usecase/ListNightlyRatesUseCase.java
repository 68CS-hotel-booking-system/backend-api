package com.team.hotelbooking.pricing.application.usecase;

import com.team.hotelbooking.pricing.application.command.ListNightlyRatesQuery;
import com.team.hotelbooking.pricing.application.result.RoomTypeNightlyRatesResult;
import java.util.List;

public interface ListNightlyRatesUseCase {

  List<RoomTypeNightlyRatesResult> execute(ListNightlyRatesQuery query);
}
