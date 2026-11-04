package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.result.ListCitiesResult;
import java.util.List;

public interface ListCitiesUseCase {

  List<ListCitiesResult> execute();
}
