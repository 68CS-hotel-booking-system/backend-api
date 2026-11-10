package com.team.hotelbooking.media.infrastructure.config;

import com.team.hotelbooking.media.application.port.HotelImageAccess;
import com.team.hotelbooking.media.application.port.ImageInspector;
import com.team.hotelbooking.media.application.port.ObjectStorage;
import com.team.hotelbooking.media.application.usecase.GetHotelImagesUseCase;
import com.team.hotelbooking.media.application.usecase.GetMediaAccessUseCase;
import com.team.hotelbooking.media.application.usecase.ReplaceHotelImagesUseCase;
import com.team.hotelbooking.media.application.usecase.UploadHotelImageUseCase;
import com.team.hotelbooking.media.application.usecase.impl.GetHotelImagesService;
import com.team.hotelbooking.media.application.usecase.impl.GetMediaAccessService;
import com.team.hotelbooking.media.application.usecase.impl.ReplaceHotelImagesService;
import com.team.hotelbooking.media.application.usecase.impl.UploadHotelImageService;
import com.team.hotelbooking.media.domain.model.MediaObject;
import com.team.hotelbooking.media.domain.repository.HotelImageAlbumRepository;
import com.team.hotelbooking.media.domain.repository.MediaRepository;
import com.team.hotelbooking.media.infrastructure.persistence.adapter.JpaHotelImageRepositoryAdapter;
import com.team.hotelbooking.media.infrastructure.persistence.adapter.JpaMediaRepositoryAdapter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "storage.r2.enabled", havingValue = "true")
public class MediaConfig {

  @Bean
  public UploadHotelImageUseCase uploadHotelImageUseCase(
      JpaMediaRepositoryAdapter repository,
      HotelImageAccess hotels,
      ImageInspector inspector,
      ObjectStorage storage,
      Clock clock,
      PlatformTransactionManager transactionManager,
      @Value("${storage.r2.bucket}") String bucket,
      @Value("${media.upload.max-bytes:5242880}") long maxBytes) {

    TransactionTemplate writes = new TransactionTemplate(transactionManager);
    writes.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    TransactionTemplate reads = new TransactionTemplate(transactionManager);
    reads.setReadOnly(true);
    reads.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    MediaRepository committedRepository =
        new MediaRepository() {

          @Override
          public void add(MediaObject media) {
            writes.executeWithoutResult(status -> repository.add(media));
          }

          @Override
          public Optional<MediaObject> findById(UUID mediaId) {
            return Objects.requireNonNull(reads.execute(status -> repository.findById(mediaId)));
          }

          @Override
          public void markReady(UUID mediaId) {
            writes.executeWithoutResult(status -> repository.markReady(mediaId));
          }

          @Override
          public void markDeleted(UUID mediaId, Instant deletedAt) {

            writes.executeWithoutResult(status -> repository.markDeleted(mediaId, deletedAt));
          }
        };

    return new UploadHotelImageService(
        committedRepository, hotels, inspector, storage, clock, bucket, maxBytes);
  }

  @Bean
  public GetHotelImagesUseCase getHotelImagesUseCase(
      HotelImageAccess hotels,
      JpaHotelImageRepositoryAdapter images,
      PlatformTransactionManager transactionManager) {

    GetHotelImagesService service = new GetHotelImagesService(hotels, images);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }

  @Bean
  public ReplaceHotelImagesUseCase replaceHotelImagesUseCase(
      HotelImageAccess hotels,
      JpaHotelImageRepositoryAdapter images,
      HotelImageAlbumRepository albums,
      Clock clock,
      PlatformTransactionManager transactionManager) {

    ReplaceHotelImagesService service =
        new ReplaceHotelImagesService(hotels, images, albums, clock);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    return command ->
        Objects.requireNonNull(transaction.execute(status -> service.execute(command)));
  }

  @Bean
  public GetMediaAccessUseCase getMediaAccessUseCase(
      JpaMediaRepositoryAdapter media,
      JpaHotelImageRepositoryAdapter images,
      HotelImageAccess hotels,
      ObjectStorage storage,
      Clock clock,
      PlatformTransactionManager transactionManager,
      @Value("${media.access.ttl:PT5M}") String ttlValue) {

    Duration ttl = Duration.parse(ttlValue);

    if (ttl.isZero() || ttl.isNegative() || ttl.compareTo(Duration.ofDays(7)) > 0) {
      throw new IllegalArgumentException("media.access.ttl phải lớn hơn 0 và tối đa 7 ngày.");
    }

    GetMediaAccessService service =
        new GetMediaAccessService(media, images, hotels, storage, clock, ttl);

    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);

    return query -> Objects.requireNonNull(transaction.execute(status -> service.execute(query)));
  }
}
