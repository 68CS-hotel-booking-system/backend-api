package com.team.hotelbooking.payment.application.usecase;

import com.team.hotelbooking.payment.application.command.GetContractsQuery;
import com.team.hotelbooking.payment.domain.model.HotelContract;
import org.springframework.data.domain.Page;

public interface GetContractsUseCase {
  Page<HotelContract> execute(GetContractsQuery query);
}
