package com.team.hotelbooking.pricing.application.usecase;

import com.team.hotelbooking.pricing.application.command.CalculateStayQuoteQuery;
import com.team.hotelbooking.pricing.application.result.StayQuoteResult;

public interface CalculateStayQuoteUseCase {

  StayQuoteResult execute(CalculateStayQuoteQuery query);
}
