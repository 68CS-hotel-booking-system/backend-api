package com.team.hotelbooking.inventory.infrastructure.config;

import com.team.hotelbooking.inventory.InventoryReservationDirectory;
import com.team.hotelbooking.inventory.application.port.BookingStayPort;
import com.team.hotelbooking.inventory.application.port.HotelCatalogPort;
import com.team.hotelbooking.inventory.application.port.HotelCoverImagePort;
import com.team.hotelbooking.inventory.application.port.HotelSearchCatalogPort;
import com.team.hotelbooking.inventory.application.port.InventoryQueryPort;
import com.team.hotelbooking.inventory.application.port.RoomPricingPort;
import com.team.hotelbooking.inventory.application.port.StayPricingPort;
import com.team.hotelbooking.inventory.application.service.CheckAvailabilityService;
import com.team.hotelbooking.inventory.application.usecase.AssignRoomsUseCase;
import com.team.hotelbooking.inventory.application.usecase.CheckAvailabilityUseCase;
import com.team.hotelbooking.inventory.application.usecase.CheckInBookingUseCase;
import com.team.hotelbooking.inventory.application.usecase.CheckOutBookingUseCase;
import com.team.hotelbooking.inventory.application.usecase.CreateRoomBlockUseCase;
import com.team.hotelbooking.inventory.application.usecase.GetRoomCalendarUseCase;
import com.team.hotelbooking.inventory.application.usecase.ListRoomBlocksUseCase;
import com.team.hotelbooking.inventory.application.usecase.MarkNoShowUseCase;
import com.team.hotelbooking.inventory.application.usecase.RecordLateArrivalUseCase;
import com.team.hotelbooking.inventory.application.usecase.ReleaseRoomBlockUseCase;
import com.team.hotelbooking.inventory.application.usecase.SearchHotelsUseCase;
import com.team.hotelbooking.inventory.application.usecase.UpdateRoomBlockUseCase;
import com.team.hotelbooking.inventory.application.usecase.UpdateRoomReadinessUseCase;
import com.team.hotelbooking.inventory.application.usecase.impl.AssignRoomsService;
import com.team.hotelbooking.inventory.application.usecase.impl.CheckInBookingService;
import com.team.hotelbooking.inventory.application.usecase.impl.CheckOutBookingService;
import com.team.hotelbooking.inventory.application.usecase.impl.CreateRoomBlockService;
import com.team.hotelbooking.inventory.application.usecase.impl.GetRoomCalendarService;
import com.team.hotelbooking.inventory.application.usecase.impl.ListRoomBlocksService;
import com.team.hotelbooking.inventory.application.usecase.impl.MarkNoShowService;
import com.team.hotelbooking.inventory.application.usecase.impl.RecordLateArrivalService;
import com.team.hotelbooking.inventory.application.usecase.impl.ReleaseRoomBlockService;
import com.team.hotelbooking.inventory.application.usecase.impl.SearchHotelsService;
import com.team.hotelbooking.inventory.application.usecase.impl.UpdateRoomBlockService;
import com.team.hotelbooking.inventory.application.usecase.impl.UpdateRoomReadinessService;
import com.team.hotelbooking.inventory.domain.repository.RoomAssignmentRepository;
import com.team.hotelbooking.inventory.domain.repository.RoomBlockRepository;
import java.time.Clock;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class InventoryConfig {

  @Bean
  public CheckAvailabilityUseCase checkAvailabilityUseCase(
      HotelCatalogPort hotelCatalogPort,
      InventoryQueryPort inventoryQueryPort,
      RoomPricingPort roomPricingPort,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    CheckAvailabilityService service =
        new CheckAvailabilityService(hotelCatalogPort, inventoryQueryPort, roomPricingPort, clock);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  // Chỉ đọc; tồn, giá và ảnh bìa lấy theo lô qua port, tham gia cùng transaction read-only này.
  @Bean
  public SearchHotelsUseCase searchHotelsUseCase(
      HotelSearchCatalogPort hotelSearchCatalogPort,
      InventoryQueryPort inventoryQueryPort,
      StayPricingPort stayPricingPort,
      HotelCoverImagePort hotelCoverImagePort,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    SearchHotelsService service =
        new SearchHotelsService(
            hotelSearchCatalogPort,
            inventoryQueryPort,
            stayPricingPort,
            hotelCoverImagePort,
            clock);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  @Bean
  public CreateRoomBlockUseCase createRoomBlockUseCase(
      HotelCatalogPort hotelCatalogPort,
      RoomBlockRepository roomBlockRepository,
      InventoryQueryPort inventoryQueryPort,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    CreateRoomBlockService service =
        new CreateRoomBlockService(
            hotelCatalogPort, roomBlockRepository, inventoryQueryPort, clock);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public UpdateRoomBlockUseCase updateRoomBlockUseCase(
      HotelCatalogPort hotelCatalogPort,
      RoomBlockRepository roomBlockRepository,
      InventoryQueryPort inventoryQueryPort,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    UpdateRoomBlockService service =
        new UpdateRoomBlockService(
            hotelCatalogPort, roomBlockRepository, inventoryQueryPort, clock);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public ReleaseRoomBlockUseCase releaseRoomBlockUseCase(
      RoomBlockRepository roomBlockRepository,
      HotelCatalogPort hotelCatalogPort,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    ReleaseRoomBlockService service =
        new ReleaseRoomBlockService(roomBlockRepository, hotelCatalogPort, clock);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public ListRoomBlocksUseCase listRoomBlocksUseCase(
      HotelCatalogPort hotelCatalogPort,
      RoomBlockRepository roomBlockRepository,
      PlatformTransactionManager transactionManager) {

    ListRoomBlocksService service =
        new ListRoomBlocksService(hotelCatalogPort, roomBlockRepository);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);
    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  @Bean
  public GetRoomCalendarUseCase getRoomCalendarUseCase(
      HotelCatalogPort hotelCatalogPort,
      RoomBlockRepository roomBlockRepository,
      RoomAssignmentRepository roomAssignmentRepository,
      InventoryQueryPort inventoryQueryPort,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    GetRoomCalendarService service =
        new GetRoomCalendarService(
            hotelCatalogPort,
            roomBlockRepository,
            roomAssignmentRepository,
            inventoryQueryPort,
            clock);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);
    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  @Bean
  public AssignRoomsUseCase assignRoomsUseCase(
      HotelCatalogPort hotelCatalogPort,
      BookingStayPort bookingStayPort,
      RoomAssignmentRepository roomAssignmentRepository,
      RoomBlockRepository roomBlockRepository,
      InventoryQueryPort inventoryQueryPort,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    AssignRoomsService service =
        new AssignRoomsService(
            hotelCatalogPort,
            bookingStayPort,
            roomAssignmentRepository,
            roomBlockRepository,
            inventoryQueryPort,
            clock);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public CheckInBookingUseCase checkInBookingUseCase(
      HotelCatalogPort hotelCatalogPort,
      BookingStayPort bookingStayPort,
      RoomAssignmentRepository roomAssignmentRepository,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    CheckInBookingService service =
        new CheckInBookingService(
            hotelCatalogPort, bookingStayPort, roomAssignmentRepository, clock);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public CheckOutBookingUseCase checkOutBookingUseCase(
      HotelCatalogPort hotelCatalogPort,
      BookingStayPort bookingStayPort,
      RoomAssignmentRepository roomAssignmentRepository,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    CheckOutBookingService service =
        new CheckOutBookingService(
            hotelCatalogPort, bookingStayPort, roomAssignmentRepository, clock);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public RecordLateArrivalUseCase recordLateArrivalUseCase(
      HotelCatalogPort hotelCatalogPort,
      BookingStayPort bookingStayPort,
      RoomAssignmentRepository roomAssignmentRepository,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    RecordLateArrivalService service =
        new RecordLateArrivalService(
            hotelCatalogPort, bookingStayPort, roomAssignmentRepository, clock);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public MarkNoShowUseCase markNoShowUseCase(
      HotelCatalogPort hotelCatalogPort,
      BookingStayPort bookingStayPort,
      RoomAssignmentRepository roomAssignmentRepository,
      InventoryReservationDirectory inventoryReservationDirectory,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    MarkNoShowService service =
        new MarkNoShowService(
            hotelCatalogPort,
            bookingStayPort,
            roomAssignmentRepository,
            inventoryReservationDirectory,
            clock);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public UpdateRoomReadinessUseCase updateRoomReadinessUseCase(
      HotelCatalogPort hotelCatalogPort,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    UpdateRoomReadinessService service = new UpdateRoomReadinessService(hotelCatalogPort, clock);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }
}
