package com.team.hotelbooking.payment.application.service;

import com.team.hotelbooking.payment.application.command.GetFinancialAccessQuery;
import com.team.hotelbooking.payment.application.port.PaymentHotelPort;
import com.team.hotelbooking.payment.application.port.PaymentIdentityPort;
import com.team.hotelbooking.payment.application.result.FinancialAccessDetailResult;
import com.team.hotelbooking.payment.application.usecase.GetFinancialAccessUseCase;
import com.team.hotelbooking.payment.domain.exception.FinanceException;
import com.team.hotelbooking.payment.domain.model.FinancialAccessDetail;
import com.team.hotelbooking.payment.domain.model.FinancialAccessHistory;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.HotelFinancialAccessHistoryJpaEntity;
import com.team.hotelbooking.payment.infrastructure.persistence.entity.HotelFinancialAccessJpaEntity;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.HotelFinancialAccessHistoryJpaRepository;
import com.team.hotelbooking.payment.infrastructure.persistence.repository.HotelFinancialAccessJpaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetFinancialAccessService implements GetFinancialAccessUseCase {

  private final HotelFinancialAccessJpaRepository accessJpaRepository;
  private final HotelFinancialAccessHistoryJpaRepository historyJpaRepository;
  private final PaymentHotelPort paymentHotelPort;
  private final PaymentIdentityPort paymentIdentityPort;

  @Override
  public FinancialAccessDetailResult execute(GetFinancialAccessQuery query) {
    if (!paymentHotelPort.existsById(query.hotelId())) {
      throw FinanceException.hotelNotFound("Không tìm thấy khách sạn.");
    }

    PaymentIdentityPort.UserSnapshot targetUser =
        paymentIdentityPort
            .findUserSnapshot(query.targetUserId())
            .orElseThrow(
                () -> FinanceException.userNotFound("Không tìm thấy người dùng được chỉ định."));

    HotelFinancialAccessJpaEntity access =
        accessJpaRepository
            .findByHotelIdAndUserId(query.hotelId(), query.targetUserId())
            .orElseThrow(
                () ->
                    FinanceException.accessNotFound(
                        "Chưa có thiết lập phân quyền tài chính cho người dùng tại khách sạn này."));

    List<HotelFinancialAccessHistoryJpaEntity> historyEntities =
        historyJpaRepository.findAllByFinancialAccessIdOrderByVersionDesc(access.getId());

    List<FinancialAccessHistory> historyList =
        historyEntities.stream()
            .map(
                h ->
                    new FinancialAccessHistory(
                        h.getId(),
                        h.getFinancialAccessId(),
                        h.getHotelId(),
                        h.getUserId(),
                        h.isGranted(),
                        h.getAccessScope(),
                        h.getReason(),
                        h.getVersion(),
                        h.getChangedByUserId(),
                        h.getChangedAt()))
            .toList();

    FinancialAccessDetail domain =
        new FinancialAccessDetail(
            access.getId(),
            access.getHotelId(),
            access.getUserId(),
            targetUser.fullName(),
            targetUser.email(),
            access.isGranted(),
            access.getAccessScope(),
            access.getReason(),
            access.getGrantedByUserId(),
            access.getGrantedAt(),
            access.getVersion(),
            historyList);

    return FinancialAccessDetailResult.from(domain);
  }
}
