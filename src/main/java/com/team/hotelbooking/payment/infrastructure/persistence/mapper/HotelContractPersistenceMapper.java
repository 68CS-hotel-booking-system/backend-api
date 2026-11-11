package com.team.hotelbooking.payment.infrastructure.persistence.mapper;

import com.team.hotelbooking.payment.domain.model.HotelContract;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.HotelContractJpaEntity;

public final class HotelContractPersistenceMapper {

  private HotelContractPersistenceMapper() {}

  public static HotelContract toDomain(HotelContractJpaEntity entity) {
    return new HotelContract(
        entity.getId(),
        entity.getContractCode(),
        entity.getHotelId(),
        entity.getOwnerUserId(),
        entity.getCommissionRate(),
        HotelContract.CommissionBasis.valueOf(entity.getCommissionBasis().name()),
        entity.getStartsAt(),
        entity.getEndsAt(),
        HotelContract.ContractStatus.valueOf(entity.getStatus().name()),
        entity.getCreatedByUserId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  // id để null cho hợp đồng mới: entity tự sinh UUID như luồng tạo trước đây.
  public static HotelContractJpaEntity toNewEntity(HotelContract contract) {
    HotelContractJpaEntity entity = new HotelContractJpaEntity();
    entity.setContractCode(contract.contractCode());
    entity.setHotelId(contract.hotelId());
    entity.setOwnerUserId(contract.ownerUserId());
    entity.setCommissionRate(contract.commissionRate());
    entity.setCommissionBasis(
        HotelContractJpaEntity.DbCommissionBasis.valueOf(contract.commissionBasis().name()));
    entity.setStartsAt(contract.startsAt());
    entity.setEndsAt(contract.endsAt());
    entity.setStatus(HotelContractJpaEntity.DbContractStatus.valueOf(contract.status().name()));
    entity.setCreatedByUserId(contract.createdByUserId());
    entity.setCreatedAt(contract.createdAt());
    entity.setUpdatedAt(contract.updatedAt());
    return entity;
  }
}
