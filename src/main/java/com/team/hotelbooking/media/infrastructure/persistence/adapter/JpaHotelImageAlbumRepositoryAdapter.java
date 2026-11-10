package com.team.hotelbooking.media.infrastructure.persistence.adapter;

import com.team.hotelbooking.media.domain.exception.MediaErrorCode;
import com.team.hotelbooking.media.domain.exception.MediaException;
import com.team.hotelbooking.media.domain.model.HotelImageAlbum;
import com.team.hotelbooking.media.domain.repository.HotelImageAlbumRepository;
import com.team.hotelbooking.media.infrastructure.persistence.entity.HotelImageAlbumJpaEntity;
import com.team.hotelbooking.media.infrastructure.persistence.mapper.HotelImageAlbumPersistenceMapper;
import com.team.hotelbooking.media.infrastructure.persistence.repository.HotelImageAlbumJpaRepository;
import com.team.hotelbooking.media.infrastructure.persistence.repository.HotelImageJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaHotelImageAlbumRepositoryAdapter implements HotelImageAlbumRepository {

  private final HotelImageAlbumJpaRepository albums;
  private final HotelImageJpaRepository images;

  @Override
  public List<HotelImageAlbum> findByHotelId(UUID hotelId) {
    return albums.findByHotelIdOrderBySortOrderAscIdAsc(hotelId).stream()
        .map(HotelImageAlbumPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public Optional<HotelImageAlbum> findByIdAndHotelId(UUID albumId, UUID hotelId) {

    return albums
        .findByIdAndHotelId(albumId, hotelId)
        .map(HotelImageAlbumPersistenceMapper::toDomain);
  }

  @Override
  public void add(HotelImageAlbum album) {
    HotelImageAlbumJpaEntity entity = HotelImageAlbumPersistenceMapper.toNewEntity(album);

    albums.saveAndFlush(entity);
  }

  @Override
  public void update(HotelImageAlbum album) {
    HotelImageAlbumJpaEntity entity = requireEntity(album.hotelId(), album.id());

    HotelImageAlbumPersistenceMapper.updateEntity(album, entity);

    albums.save(entity);
  }

  @Override
  public void deleteAndUngroupImages(UUID hotelId, UUID albumId) {

    HotelImageAlbumJpaEntity entity = requireEntity(hotelId, albumId);

    // Gỡ liên kết trước để không vi phạm khóa ngoại.
    images.ungroupImages(hotelId, albumId);

    // Chỉ xóa album; không xóa ảnh hoặc file R2.
    albums.delete(entity);
    albums.flush();
  }

  private HotelImageAlbumJpaEntity requireEntity(UUID hotelId, UUID albumId) {

    return albums
        .findByIdAndHotelId(albumId, hotelId)
        .orElseThrow(
            () ->
                new MediaException(
                    MediaErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy album trong khách sạn."));
  }
}
