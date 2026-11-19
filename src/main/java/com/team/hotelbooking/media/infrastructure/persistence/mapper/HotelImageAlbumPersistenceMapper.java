package com.team.hotelbooking.media.infrastructure.persistence.mapper;

import com.team.hotelbooking.media.domain.model.HotelImageAlbum;
import com.team.hotelbooking.media.infrastructure.persistence.entity.HotelImageAlbumJpaEntity;

public final class HotelImageAlbumPersistenceMapper {

  private HotelImageAlbumPersistenceMapper() {}

  public static HotelImageAlbum toDomain(HotelImageAlbumJpaEntity entity) {

    return new HotelImageAlbum(
        entity.getId(),
        entity.getHotelId(),
        entity.getName(),
        entity.getDescription(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  public static HotelImageAlbumJpaEntity toNewEntity(HotelImageAlbum album) {

    HotelImageAlbumJpaEntity entity = new HotelImageAlbumJpaEntity();

    entity.setId(album.id());
    entity.setHotelId(album.hotelId());
    entity.setName(album.name());
    entity.setDescription(album.description());
    entity.setSortOrder(album.sortOrder());
    entity.setCreatedAt(album.createdAt());
    entity.setUpdatedAt(album.updatedAt());

    return entity;
  }

  public static void updateEntity(HotelImageAlbum album, HotelImageAlbumJpaEntity entity) {

    entity.setName(album.name());
    entity.setDescription(album.description());
    entity.setSortOrder(album.sortOrder());
    entity.setUpdatedAt(album.updatedAt());
  }
}
