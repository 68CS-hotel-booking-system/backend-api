package com.team.hotelbooking.payment.application.usecase.impl;

import com.team.hotelbooking.payment.application.command.RenewContractCommand;
import com.team.hotelbooking.payment.application.result.HotelContractResult;
import com.team.hotelbooking.payment.application.usecase.CreateContractUseCase;
import com.team.hotelbooking.payment.application.usecase.RenewContractUseCase;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.domain.model.HotelContract;
import com.team.hotelbooking.payment.domain.repository.HotelContractRepository;

public class RenewContractService implements RenewContractUseCase {
  private final HotelContractRepository contracts;
  private final CreateContractUseCase create;

  public RenewContractService(HotelContractRepository contracts, CreateContractUseCase create) {
    this.contracts = contracts;
    this.create = create;
  }

  @Override
  public HotelContractResult execute(RenewContractCommand command) {
    var next = command.replacement();
    var previous =
        ContractAccess.lockContract(
            contracts,
            new com.team.hotelbooking.payment.application.command.ChangeContractStatusCommand(
                next.currentAdminUserId(), next.hotelId(), command.previousContractId()));
    if (previous.status() == HotelContract.ContractStatus.DRAFT) {
      throw FinanceException.invalidPeriod(
          "Hợp đồng dự thảo cần được kích hoạt trước khi gia hạn.");
    }
    if (next.startsAt() == null || next.startsAt().isBefore(previous.endsAt())) {
      throw FinanceException.invalidPeriod(
          "Hợp đồng kế tiếp phải bắt đầu từ khi hợp đồng trước kết thúc.");
    }
    var result = create.execute(next);
    contracts.recordRenewal(previous.id(), result.id());
    return result;
  }
}
