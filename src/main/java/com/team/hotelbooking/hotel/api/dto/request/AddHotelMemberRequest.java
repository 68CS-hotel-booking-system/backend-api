package com.team.hotelbooking.hotel.api.dto.request;

import com.team.hotelbooking.hotel.domain.model.HotelMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddHotelMemberRequest(
    @NotBlank(message = "Email tài khoản là bắt buộc.")
        @Email(message = "Email không đúng định dạng.")
        @Size(max = 255, message = "Email không được vượt quá 255 ký tự.")
        String email,
    @NotNull(message = "Vai trò nhân sự là bắt buộc.") HotelMemberRole memberRole) {}
