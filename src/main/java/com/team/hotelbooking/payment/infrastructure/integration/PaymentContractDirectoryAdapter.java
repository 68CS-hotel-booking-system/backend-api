package com.team.hotelbooking.payment.infrastructure.integration;

import com.team.hotelbooking.payment.ContractDirectory;
import com.team.hotelbooking.payment.domain.model.HotelContract;
import com.team.hotelbooking.payment.domain.repository.HotelContractRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

// Không bọc transaction riêng: module gọi tham gia transaction use case của chính nó.
public class PaymentContractDirectoryAdapter implements ContractDirectory {

  private final HotelContractRepository contracts;

  public PaymentContractDirectoryAdapter(HotelContractRepository contracts) {
    this.contracts = contracts;
  }

  @Override
  public Optional<ContractSnapshot> findEffective(UUID hotelId, Instant at) {
    return contracts
        .findEffective(hotelId, at)
        .map(
            contract ->
                new ContractSnapshot(
                    contract.id(),
                    contract.ownerUserId(),
                    contract.commissionRate(),
                    toBasis(contract.commissionBasis())));
  }

  private static CommissionBasis toBasis(HotelContract.CommissionBasis basis) {
    return switch (basis) {
      case BEFORE_DISCOUNT -> CommissionBasis.BEFORE_DISCOUNT;
      case AFTER_DISCOUNT -> CommissionBasis.AFTER_DISCOUNT;
      case AFTER_HOTEL_DISCOUNT -> CommissionBasis.AFTER_HOTEL_DISCOUNT;
    };
  }
}
