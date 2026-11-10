package com.team.hotelbooking.media.infrastructure.persistence.mapper;

import com.team.hotelbooking.media.domain.model.MediaObject;
import com.team.hotelbooking.media.infrastructure.persistence.entity.MediaJpaEntity;

public final class MediaPersistenceMapper {

  private MediaPersistenceMapper() {}

  public static MediaJpaEntity toNewEntity(MediaObject media) {
    MediaJpaEntity entity = new MediaJpaEntity();

    entity.setId(media.id());
    entity.setStorageProvider(media.storageProvider());
    entity.setBucketName(media.bucketName());
    entity.setObjectKey(media.objectKey());
    entity.setOriginalFilename(media.originalFilename());
    entity.setContentType(media.contentType());
    entity.setSizeBytes(media.sizeBytes());
    entity.setChecksumSha256(media.checksumSha256());
    entity.setPurpose(media.purpose().name());
    entity.setHotelId(media.hotelId());
    entity.setStatus(MediaJpaEntity.DbStatus.valueOf(media.status().name()));
    entity.setCreatedByUserId(media.createdByUserId());
    entity.setCreatedAt(media.createdAt());
    entity.setDeletedAt(media.deletedAt());

    return entity;
  }

  public static MediaObject toDomain(MediaJpaEntity entity) {
    return new MediaObject(
        entity.getId(),
        entity.getStorageProvider(),
        entity.getBucketName(),
        entity.getObjectKey(),
        entity.getOriginalFilename(),
        entity.getContentType(),
        entity.getSizeBytes(),
        entity.getChecksumSha256(),
        MediaObject.Purpose.valueOf(entity.getPurpose()),
        entity.getHotelId(),
        MediaObject.Status.valueOf(entity.getStatus().name()),
        entity.getCreatedByUserId(),
        entity.getCreatedAt(),
        entity.getDeletedAt());
  }
}
