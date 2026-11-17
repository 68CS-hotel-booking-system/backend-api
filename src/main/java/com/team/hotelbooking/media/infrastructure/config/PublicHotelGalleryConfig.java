package com.team.hotelbooking.media.infrastructure.config;

import com.team.hotelbooking.media.application.port.HotelImageAccess;
import com.team.hotelbooking.media.application.usecase.GetPublicHotelGalleryUseCase;
import com.team.hotelbooking.media.application.usecase.impl.GetPublicHotelGalleryService;
import com.team.hotelbooking.media.domain.repository.HotelImageAlbumRepository;
import com.team.hotelbooking.media.domain.repository.HotelImageRepository;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
public class PublicHotelGalleryConfig {
  @Bean
  public GetPublicHotelGalleryUseCase getPublicHotelGalleryUseCase(
      HotelImageAccess hotels,
      HotelImageRepository images,
      HotelImageAlbumRepository albums,
      PlatformTransactionManager manager) {
    var service = new GetPublicHotelGalleryService(hotels, images, albums);
    var transaction = new TransactionTemplate(manager);
    transaction.setReadOnly(true);
    return hotelId ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(hotelId)));
  }
}
