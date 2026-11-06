package com.team.hotelbooking.identity.application.usecase.impl;

import com.team.hotelbooking.identity.application.command.CreateAdminUserCommand;
import com.team.hotelbooking.identity.application.result.AdminUserResult;
import com.team.hotelbooking.identity.application.usecase.CreateAdminUserUseCase;
import com.team.hotelbooking.identity.domain.exception.IdentityErrorCode;
import com.team.hotelbooking.identity.domain.exception.IdentityException;

/**
 * Khung xử lý; bổ sung dependency qua constructor và bean trong IdentityConfig khi viết nghiệp vụ.
 */
public final class CreateAdminUserService implements CreateAdminUserUseCase {

  @Override
  public AdminUserResult execute(CreateAdminUserCommand command) {
    // TODO 1: Kiểm tra quyền, chuẩn hóa dữ liệu, kiểm tra email/điện thoại trùng và vai trò hợp lệ.
    // TODO 2: Chốt cơ chế khởi tạo mật khẩu hoặc lời mời đặt mật khẩu; request chưa nhận mật khẩu.
    // TODO 3: Tạo và lưu tài khoản/vai trò; dùng các port hiện có nếu cần gửi email sau commit.
    // TODO 4: Trả AdminUserResult từ tài khoản đã lưu.
    throw new IdentityException(IdentityErrorCode.NOT_IMPLEMENTED);
  }
}
