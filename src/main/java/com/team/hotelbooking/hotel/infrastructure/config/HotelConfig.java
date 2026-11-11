package com.team.hotelbooking.hotel.infrastructure.config;

import com.team.hotelbooking.hotel.application.port.IdentityAccounts;
import com.team.hotelbooking.hotel.application.port.RoomInventoryPort;
import com.team.hotelbooking.hotel.application.usecase.ApproveHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.CreateHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.CreateRoomTypeUseCase;
import com.team.hotelbooking.hotel.application.usecase.CreateRoomUseCase;
import com.team.hotelbooking.hotel.application.usecase.GetManagementHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.GetPublicHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListAmenitiesUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListCitiesUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListManagementHotelsUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListManagementRoomTypesUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListManagementRoomsUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListPublicHotelsUseCase;
import com.team.hotelbooking.hotel.application.usecase.ListPublicRoomTypesUseCase;
import com.team.hotelbooking.hotel.application.usecase.LockHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.ReplaceHotelAmenitiesUseCase;
import com.team.hotelbooking.hotel.application.usecase.ReplaceRoomTypeAmenitiesUseCase;
import com.team.hotelbooking.hotel.application.usecase.ResumeHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.SuspendHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.UnlockHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.UpdateHotelUseCase;
import com.team.hotelbooking.hotel.application.usecase.UpdateRoomTypeUseCase;
import com.team.hotelbooking.hotel.application.usecase.UpdateRoomUseCase;
import com.team.hotelbooking.hotel.application.usecase.impl.ApproveHotelService;
import com.team.hotelbooking.hotel.application.usecase.impl.CreateHotelService;
import com.team.hotelbooking.hotel.application.usecase.impl.CreateRoomService;
import com.team.hotelbooking.hotel.application.usecase.impl.CreateRoomTypeService;
import com.team.hotelbooking.hotel.application.usecase.impl.GetManagementHotelService;
import com.team.hotelbooking.hotel.application.usecase.impl.GetPublicHotelService;
import com.team.hotelbooking.hotel.application.usecase.impl.ListAmenitiesService;
import com.team.hotelbooking.hotel.application.usecase.impl.ListCitiesService;
import com.team.hotelbooking.hotel.application.usecase.impl.ListManagementHotelsService;
import com.team.hotelbooking.hotel.application.usecase.impl.ListManagementRoomTypesService;
import com.team.hotelbooking.hotel.application.usecase.impl.ListManagementRoomsService;
import com.team.hotelbooking.hotel.application.usecase.impl.ListPublicHotelsService;
import com.team.hotelbooking.hotel.application.usecase.impl.ListPublicRoomTypesService;
import com.team.hotelbooking.hotel.application.usecase.impl.LockHotelService;
import com.team.hotelbooking.hotel.application.usecase.impl.ReplaceHotelAmenitiesService;
import com.team.hotelbooking.hotel.application.usecase.impl.ReplaceRoomTypeAmenitiesService;
import com.team.hotelbooking.hotel.application.usecase.impl.ResumeHotelService;
import com.team.hotelbooking.hotel.application.usecase.impl.SuspendHotelService;
import com.team.hotelbooking.hotel.application.usecase.impl.UnlockHotelService;
import com.team.hotelbooking.hotel.application.usecase.impl.UpdateHotelService;
import com.team.hotelbooking.hotel.application.usecase.impl.UpdateRoomService;
import com.team.hotelbooking.hotel.application.usecase.impl.UpdateRoomTypeService;
import com.team.hotelbooking.hotel.domain.repository.AmenityRepository;
import com.team.hotelbooking.hotel.domain.repository.CityRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelRepository;
import com.team.hotelbooking.hotel.domain.repository.HotelStatusHistoryRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomRepository;
import com.team.hotelbooking.hotel.domain.repository.RoomTypeRepository;
import com.team.hotelbooking.shared.config.PaginationProperties;
import java.time.Clock;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class HotelConfig {

  @Bean
  public CreateRoomUseCase createRoomUseCase(
      HotelRepository hotels,
      RoomTypeRepository roomTypes,
      RoomRepository rooms,
      IdentityAccounts accounts,
      Clock clock,
      PlatformTransactionManager transactionManager) {
    CreateRoomService service = new CreateRoomService(hotels, roomTypes, rooms, accounts, clock);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public CreateRoomTypeUseCase createRoomTypeUseCase(
      HotelRepository hotels,
      RoomTypeRepository roomTypes,
      IdentityAccounts accounts,
      Clock clock,
      PlatformTransactionManager transactionManager) {
    CreateRoomTypeService service = new CreateRoomTypeService(hotels, roomTypes, accounts, clock);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public CreateHotelUseCase createHotelUseCase(
      HotelRepository hotels,
      CityRepository cities,
      IdentityAccounts accounts,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    CreateHotelService service = new CreateHotelService(hotels, cities, accounts, clock);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public GetManagementHotelUseCase getManagementHotelUseCase(
      HotelRepository hotels,
      IdentityAccounts accounts,
      PlatformTransactionManager transactionManager) {
    GetManagementHotelService service = new GetManagementHotelService(hotels, accounts);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  @Bean
  public ListManagementHotelsUseCase listManagementHotelsUseCase(
      HotelRepository hotels,
      IdentityAccounts accounts,
      PaginationProperties pagination,
      PlatformTransactionManager transactionManager) {
    ListManagementHotelsService service =
        new ListManagementHotelsService(hotels, accounts, pagination.maxSize());
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  @Bean
  public UpdateRoomTypeUseCase updateRoomTypeUseCase(
      HotelRepository hotels,
      RoomTypeRepository roomTypes,
      Clock clock,
      PlatformTransactionManager transactionManager) {
    UpdateRoomTypeService service = new UpdateRoomTypeService(hotels, roomTypes, clock);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  // Kiểm trùng số phòng và ghi phòng trong cùng transaction; lỗi ở bước nào cũng rollback.
  @Bean
  public UpdateRoomUseCase updateRoomUseCase(
      HotelRepository hotels,
      RoomRepository rooms,
      RoomInventoryPort inventory,
      Clock clock,
      PlatformTransactionManager transactionManager) {
    UpdateRoomService service = new UpdateRoomService(hotels, rooms, clock, inventory);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public ListCitiesUseCase listCitiesUseCase(
      CityRepository cities, PlatformTransactionManager transactionManager) {
    ListCitiesService service = new ListCitiesService(cities);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return () -> Objects.requireNonNull(transaction.execute(status -> service.execute()));
  }

  @Bean
  public ListAmenitiesUseCase listAmenitiesUseCase(
      AmenityRepository amenities, PlatformTransactionManager transactionManager) {
    ListAmenitiesService service = new ListAmenitiesService(amenities);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return category ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(category)));
  }

  @Bean
  public GetPublicHotelUseCase getPublicHotelUseCase(
      HotelRepository hotels,
      AmenityRepository amenities,
      PlatformTransactionManager transactionManager) {
    GetPublicHotelService service = new GetPublicHotelService(hotels, amenities);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return hotelId ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(hotelId)));
  }

  @Bean
  public ListPublicRoomTypesUseCase listPublicRoomTypesUseCase(
      HotelRepository hotels,
      RoomTypeRepository roomTypes,
      AmenityRepository amenities,
      PlatformTransactionManager transactionManager) {
    ListPublicRoomTypesService service =
        new ListPublicRoomTypesService(hotels, roomTypes, amenities);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return hotelId ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(hotelId)));
  }

  @Bean
  public ListManagementRoomTypesUseCase listManagementRoomTypesUseCase(
      HotelRepository hotels,
      RoomTypeRepository roomTypes,
      IdentityAccounts accounts,
      PlatformTransactionManager transactionManager) {
    ListManagementRoomTypesService service =
        new ListManagementRoomTypesService(hotels, roomTypes, accounts);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  @Bean
  public ListManagementRoomsUseCase listManagementRoomsUseCase(
      HotelRepository hotels,
      RoomRepository rooms,
      IdentityAccounts accounts,
      PaginationProperties pagination,
      PlatformTransactionManager transactionManager) {
    ListManagementRoomsService service =
        new ListManagementRoomsService(hotels, rooms, accounts, pagination.maxSize());
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  // Duyệt, kiểm HT-03 và ghi lịch sử trong cùng transaction; lỗi ở bước nào cũng rollback.
  @Bean
  public ApproveHotelUseCase approveHotelUseCase(
      HotelRepository hotels,
      RoomTypeRepository roomTypes,
      RoomRepository rooms,
      HotelStatusHistoryRepository statusHistory,
      IdentityAccounts accounts,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    ApproveHotelService service =
        new ApproveHotelService(hotels, roomTypes, rooms, statusHistory, accounts, clock);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return (hotelId, adminActorId) ->
        Objects.requireNonNull(
            transaction.execute(status -> service.execute(hotelId, adminActorId)));
  }

  @Bean
  public ListPublicHotelsUseCase listPublicHotelsUseCase(
      HotelRepository hotels, PlatformTransactionManager transactionManager) {
    ListPublicHotelsService service = new ListPublicHotelsService(hotels);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  @Bean
  public UpdateHotelUseCase updateHotelUseCase(
      HotelRepository hotels, Clock clock, PlatformTransactionManager transactionManager) {
    UpdateHotelService service = new UpdateHotelService(hotels, clock);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  // Xóa danh sách cũ và ghi danh sách mới trong cùng transaction; lỗi ở bước nào cũng rollback.
  @Bean
  public ReplaceHotelAmenitiesUseCase replaceHotelAmenitiesUseCase(
      HotelRepository hotels,
      AmenityRepository amenities,
      PlatformTransactionManager transactionManager) {
    ReplaceHotelAmenitiesService service = new ReplaceHotelAmenitiesService(hotels, amenities);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public ReplaceRoomTypeAmenitiesUseCase replaceRoomTypeAmenitiesUseCase(
      HotelRepository hotels,
      RoomTypeRepository roomTypes,
      AmenityRepository amenities,
      PlatformTransactionManager transactionManager) {
    ReplaceRoomTypeAmenitiesService service =
        new ReplaceRoomTypeAmenitiesService(hotels, roomTypes, amenities);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  // Đổi trạng thái bán và ghi lịch sử trong cùng transaction; lỗi ở bước nào cũng rollback cả hai.
  @Bean
  public SuspendHotelUseCase suspendHotelUseCase(
      HotelRepository hotels,
      RoomTypeRepository roomTypes,
      RoomRepository rooms,
      HotelStatusHistoryRepository statusHistory,
      Clock clock,
      PlatformTransactionManager transactionManager) {
    SuspendHotelService service =
        new SuspendHotelService(hotels, roomTypes, rooms, statusHistory, clock);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public ResumeHotelUseCase resumeHotelUseCase(
      HotelRepository hotels,
      RoomTypeRepository roomTypes,
      RoomRepository rooms,
      HotelStatusHistoryRepository statusHistory,
      Clock clock,
      PlatformTransactionManager transactionManager) {
    ResumeHotelService service =
        new ResumeHotelService(hotels, roomTypes, rooms, statusHistory, clock);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public LockHotelUseCase lockHotelUseCase(
      HotelRepository hotels,
      RoomTypeRepository roomTypes,
      RoomRepository rooms,
      HotelStatusHistoryRepository statusHistory,
      Clock clock,
      PlatformTransactionManager transactionManager) {
    LockHotelService service = new LockHotelService(hotels, roomTypes, rooms, statusHistory, clock);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public UnlockHotelUseCase unlockHotelUseCase(
      HotelRepository hotels,
      RoomTypeRepository roomTypes,
      RoomRepository rooms,
      HotelStatusHistoryRepository statusHistory,
      Clock clock,
      PlatformTransactionManager transactionManager) {
    UnlockHotelService service =
        new UnlockHotelService(hotels, roomTypes, rooms, statusHistory, clock);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }
}
