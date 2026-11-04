package com.team.hotelbooking.payment.application.usecase;

import com.team.hotelbooking.payment.application.command.GetContractsQuery;
import com.team.hotelbooking.payment.application.result.HotelContractResult;
import org.springframework.data.domain.Page;

public interface GetContractsUseCase {
  Page<HotelContractResult> execute(GetContractsQuery query);
}
