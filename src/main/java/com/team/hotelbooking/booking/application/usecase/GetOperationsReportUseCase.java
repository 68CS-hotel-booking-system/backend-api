package com.team.hotelbooking.booking.application.usecase;

import com.team.hotelbooking.booking.application.command.GetOperationsReportQuery;
import com.team.hotelbooking.booking.application.result.OperationsReportResult;

public interface GetOperationsReportUseCase {

  OperationsReportResult execute(GetOperationsReportQuery query);
}
