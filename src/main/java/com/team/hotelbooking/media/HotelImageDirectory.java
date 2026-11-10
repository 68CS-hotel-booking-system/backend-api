package com.team.hotelbooking.media;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Ảnh khách sạn cho module khác (tìm kiếm ở inventory). Chỉ trả mediaId; FE lấy URL có hạn qua GET
 * /api/v1/media/{mediaId}/access (đã kiểm MD-06). Không gọi R2.
 *
 * <p>Không tự kiểm trạng thái khách sạn: bên gọi chỉ truyền khách sạn đã kiểm ACTIVE (MD-06).
 */
public interface HotelImageDirectory {

  // Theo lô: ảnh bìa (is_cover) đã READY của từng khách sạn; khách sạn chưa có ảnh bìa không có
  // trong map để FE dùng ảnh mặc định (MD-05).
  Map<UUID, UUID> findCoverImages(Collection<UUID> hotelIds);
}
