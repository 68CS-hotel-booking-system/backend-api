package com.team.hotelbooking.payment.application.usecase.impl;

import com.team.hotelbooking.payment.application.command.ChangeContractStatusCommand;
import com.team.hotelbooking.payment.application.result.HotelContractResult;
import com.team.hotelbooking.payment.application.usecase.TerminateContractUseCase;
import com.team.hotelbooking.payment.domain.model.HotelContract;
import com.team.hotelbooking.payment.domain.repository.HotelContractRepository;
import java.time.Clock;

// PM-16: chấm dứt hợp đồng DRAFT/ACTIVE; giữ nguyên điều khoản, booking đã chốt giữ snapshot cũ.
public final class TerminateContractService implements TerminateContractUseCase {

  private final HotelContractRepository contracts;
  private final Clock clock;

  public TerminateContractService(HotelContractRepository contracts, Clock clock) {
    this.contracts = contracts;
    this.clock = clock;
  }

  @Override
  public HotelContractResult execute(ChangeContractStatusCommand command) {
    HotelContract contract = ContractAccess.lockContract(contracts, command);

    return HotelContractResult.from(contracts.updateStatus(contract.terminate(clock.instant())));
  }
}
