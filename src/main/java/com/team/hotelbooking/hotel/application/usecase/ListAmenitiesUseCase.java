package com.team.hotelbooking.hotel.application.usecase;

import com.team.hotelbooking.hotel.application.result.ListAmenitiesResult;
import java.util.List;

public interface ListAmenitiesUseCase {

  List<ListAmenitiesResult> execute(String category);
}
