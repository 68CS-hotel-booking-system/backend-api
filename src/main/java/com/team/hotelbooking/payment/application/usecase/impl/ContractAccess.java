package com.team.hotelbooking.payment.application.usecase.impl;

import com.team.hotelbooking.payment.application.command.ChangeContractStatusCommand;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.domain.model.HotelContract;
import com.team.hotelbooking.payment.domain.repository.HotelContractRepository;

// Bước chung của kích hoạt/chấm dứt: có người thực hiện, khóa đúng hợp đồng của đúng khách sạn.
final class ContractAccess {

  private ContractAccess() {}

  static HotelContract lockContract(
      HotelContractRepository contracts, ChangeContractStatusCommand command) {
    if (command == null || command.actorUserId() == null) {
      throw FinanceException.accessDenied("Chỉ quản trị hệ thống được đổi trạng thái hợp đồng.");
    }
    return contracts
        .findByIdAndHotelIdForUpdate(command.contractId(), command.hotelId())
        .orElseThrow(
            () -> FinanceException.contractNotFound("Không tìm thấy hợp đồng của khách sạn."));
  }
}
