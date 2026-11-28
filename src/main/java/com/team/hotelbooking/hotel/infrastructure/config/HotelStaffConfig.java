package com.team.hotelbooking.hotel.infrastructure.config;

import com.team.hotelbooking.hotel.application.port.HotelMemberStore;
import com.team.hotelbooking.hotel.application.port.HotelMemberStore.MemberRecord;
import com.team.hotelbooking.hotel.application.port.HotelMemberStore.StaffHotelRecord;
import com.team.hotelbooking.hotel.application.port.HotelMemberStore.WorkplaceRecord;
import com.team.hotelbooking.hotel.application.port.IdentityAccounts;
import com.team.hotelbooking.hotel.application.usecase.HotelStaffUseCase;
import com.team.hotelbooking.hotel.application.usecase.impl.HotelStaffService;
import com.team.hotelbooking.hotel.domain.model.HotelMemberRole;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.shared.config.PaginationProperties;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class HotelStaffConfig {

  @Bean
  public HotelStaffUseCase hotelStaffUseCase(
      HotelRepository hotels,
      HotelMemberStore members,
      IdentityAccounts accounts,
      Clock clock,
      PaginationProperties pagination,
      PlatformTransactionManager transactionManager) {
    HotelStaffService service =
        new HotelStaffService(hotels, members, accounts, clock, pagination.maxSize());
    TransactionTemplate readTx = new TransactionTemplate(transactionManager);
    readTx.setReadOnly(true);
    TransactionTemplate writeTx = new TransactionTemplate(transactionManager);

    return new HotelStaffUseCase() {
      @Override
      public PageResult<StaffHotelRecord> listStaffHotels(UUID actorId, PageQuery pageQuery) {
        return Objects.requireNonNull(
            readTx.execute(status -> service.listStaffHotels(actorId, pageQuery)));
      }

      @Override
      public PageResult<MemberRecord> listMembers(
          UUID actorId, UUID hotelId, PageQuery pageQuery) {
        return Objects.requireNonNull(
            readTx.execute(status -> service.listMembers(actorId, hotelId, pageQuery)));
      }

      @Override
      public MemberRecord addMember(
          UUID actorId, UUID hotelId, String email, HotelMemberRole memberRole) {
        return Objects.requireNonNull(
            writeTx.execute(status -> service.addMember(actorId, hotelId, email, memberRole)));
      }

      @Override
      public MemberRecord updateMemberStatus(
          UUID actorId, UUID hotelId, UUID memberId, String statusValue, long expectedVersion) {
        return Objects.requireNonNull(
            writeTx.execute(
                status ->
                    service.updateMemberStatus(
                        actorId, hotelId, memberId, statusValue, expectedVersion)));
      }

      @Override
      public List<WorkplaceRecord> listWorkplaces(UUID actorId) {
        return Objects.requireNonNull(readTx.execute(status -> service.listWorkplaces(actorId)));
      }
    };
  }
}
