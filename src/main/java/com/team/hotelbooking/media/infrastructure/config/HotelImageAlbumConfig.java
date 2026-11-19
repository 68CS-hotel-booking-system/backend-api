package com.team.hotelbooking.media.infrastructure.config;

import com.team.hotelbooking.media.application.port.HotelImageAccess;
import com.team.hotelbooking.media.application.usecase.CreateHotelImageAlbumUseCase;
import com.team.hotelbooking.media.application.usecase.DeleteHotelImageAlbumUseCase;
import com.team.hotelbooking.media.application.usecase.ListHotelImageAlbumsUseCase;
import com.team.hotelbooking.media.application.usecase.ReorderHotelImageAlbumsUseCase;
import com.team.hotelbooking.media.application.usecase.UpdateHotelImageAlbumUseCase;
import com.team.hotelbooking.media.application.usecase.impl.HotelImageAlbumService;
import com.team.hotelbooking.media.domain.repository.HotelImageAlbumRepository;
import java.time.Clock;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "storage.r2.enabled", havingValue = "true")
public class HotelImageAlbumConfig {

  private static HotelImageAlbumService service(
      HotelImageAlbumRepository albums, HotelImageAccess hotels, Clock clock) {

    return new HotelImageAlbumService(albums, hotels, clock);
  }

  @Bean
  public ListHotelImageAlbumsUseCase listHotelImageAlbumsUseCase(
      HotelImageAlbumRepository albums,
      HotelImageAccess hotels,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    HotelImageAlbumService service = service(albums, hotels, clock);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  @Bean
  public CreateHotelImageAlbumUseCase createHotelImageAlbumUseCase(
      HotelImageAlbumRepository albums,
      HotelImageAccess hotels,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    HotelImageAlbumService service = service(albums, hotels, clock);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public UpdateHotelImageAlbumUseCase updateHotelImageAlbumUseCase(
      HotelImageAlbumRepository albums,
      HotelImageAccess hotels,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    HotelImageAlbumService service = service(albums, hotels, clock);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public ReorderHotelImageAlbumsUseCase reorderHotelImageAlbumsUseCase(
      HotelImageAlbumRepository albums,
      HotelImageAccess hotels,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    HotelImageAlbumService service = service(albums, hotels, clock);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public DeleteHotelImageAlbumUseCase deleteHotelImageAlbumUseCase(
      HotelImageAlbumRepository albums,
      HotelImageAccess hotels,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    HotelImageAlbumService service = service(albums, hotels, clock);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }
}
