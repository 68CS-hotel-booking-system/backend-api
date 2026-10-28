package com.team.hotelbooking.payment.application.service;

import com.team.hotelbooking.hotel.infrastructure.persistence.repository.HotelJpaRepository;
import com.team.hotelbooking.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.team.hotelbooking.identity.infrastructure.persistence.repository.UserJpaRepository;
import com.team.hotelbooking.payment.application.command.GetFinancialAccessQuery;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetFinancialAccessService implements GetFinancialAccessUseCase {

  private final HotelJpaRepository hotelJpaRepository;
  private final UserJpaRepository userJpaRepository;
  private final HotelFinancialAccessJpaRepository accessJpaRepository;
  private final HotelFinancialAccessHistoryJpaRepository historyJpaRepository;

  @Override
  public FinancialAccessDetail execute(GetFinancialAccessQuery query) {
    if (!hotelJpaRepository.existsById(query.hotelId())) {
      throw FinanceException.hotelNotFound("Không tìm thấy khách sạn.");
    }

    UserJpaEntity targetUser =
        userJpaRepository
            .findById(query.targetUserId())
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

    return new FinancialAccessDetail(
        access.getId(),
        access.getHotelId(),
        access.getUserId(),
        targetUser.getFullName(),
        targetUser.getEmail(),
        access.isGranted(),
        access.getAccessScope(),
        access.getReason(),
        access.getGrantedByUserId(),
        access.getGrantedAt(),
        access.getVersion(),
        historyList);
  }
}
