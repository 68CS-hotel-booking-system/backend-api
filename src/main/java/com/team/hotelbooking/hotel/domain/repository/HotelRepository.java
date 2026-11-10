package com.team.hotelbooking.hotel.domain.repository;

import com.team.hotelbooking.hotel.domain.model.Hotel;
import com.team.hotelbooking.hotel.domain.model.HotelMemberRole;
import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface HotelRepository {

  Hotel add(Hotel hotel);

  Hotel save(Hotel hotel);

  Optional<Hotel> findById(UUID hotelId);

  // Khóa ghi hàng hotel để các thao tác thay danh sách con của cùng khách sạn chạy tuần tự.
  Optional<Hotel> findByIdForUpdate(UUID hotelId);

  // Ghi đè thông tin hotel nếu version hiện tại bằng expectedVersion; lệch thì báo xung đột.
  Hotel update(Hotel hotel, long expectedVersion);

  // Chỉ ghi trạng thái bán và updatedAt; gọi sau findByIdForUpdate trong cùng transaction.
  Hotel updateStatus(Hotel hotel);

  // Các vai trò hotel_members đang hoạt động của user tại đúng hotel; rỗng nếu không là thành viên.
  Set<HotelMemberRole> findActiveMemberRoles(UUID hotelId, UUID userId);

  Optional<Hotel> findByIdAndOwnerId(UUID hotelId, UUID ownerId);

  Optional<Hotel> findByIdAndStatus(UUID hotelId, Hotel.Status status);

  PageResult<Hotel> findAll(PageQuery pagination);

  PageResult<Hotel> findByOwnerId(UUID ownerId, PageQuery pagination);

  PageResult<Hotel> findPublicHotels(UUID cityId, String keyword, PageQuery pagination);
}
