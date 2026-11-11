package com.team.hotelbooking.payment.application.usecase.impl;

import com.team.hotelbooking.payment.application.command.ChangeContractStatusCommand;
import com.team.hotelbooking.payment.application.result.HotelContractResult;
import com.team.hotelbooking.payment.application.usecase.ActivateContractUseCase;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.domain.model.HotelContract;
import com.team.hotelbooking.payment.domain.repository.HotelContractRepository;
import java.time.Clock;

/**
 * PM-16: kích hoạt hợp đồng DRAFT. Không cho hai hợp đồng ACTIVE chồng hiệu lực trên cùng khách
 * sạn: kiểm trước (lỗi rõ ràng) và ràng buộc exclusion của DB (hai giao dịch song song).
 */
public final class ActivateContractService implements ActivateContractUseCase {

  private final HotelContractRepository contracts;
  private final Clock clock;

  public ActivateContractService(HotelContractRepository contracts, Clock clock) {
    this.contracts = contracts;
    this.clock = clock;
  }

  @Override
  public HotelContractResult execute(ChangeContractStatusCommand command) {
    HotelContract contract = ContractAccess.lockContract(contracts, command);

    HotelContract activated = contract.activate(clock.instant());

    if (contracts.hasOverlappingActive(
        contract.hotelId(), contract.startsAt(), contract.endsAt(), contract.id())) {
      throw FinanceException.activeContractOverlap(
          "Khoảng thời gian hiệu lực bị trùng lặp với hợp đồng đang hoạt động khác của khách sạn.");
    }

    return HotelContractResult.from(contracts.updateStatus(activated));
  }
}
