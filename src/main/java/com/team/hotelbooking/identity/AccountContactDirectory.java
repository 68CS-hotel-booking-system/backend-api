package com.team.hotelbooking.identity;

import com.team.hotelbooking.shared.pagination.PageQuery;
import com.team.hotelbooking.shared.pagination.PageResult;
import java.util.Set;
import java.util.UUID;

/** Tra cứu liên hệ trong tập tài khoản đã được module gọi xác định phạm vi nghiệp vụ. */
public interface AccountContactDirectory {
  PageResult<Contact> findByIds(Set<UUID> ids, String keyword, PageQuery pagination);

  record Contact(UUID id, String fullName, String email, String phone) {}
}
