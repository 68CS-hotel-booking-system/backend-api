package com.team.hotelbooking.payment.application.command;

import java.util.UUID;

// Dùng cho kích hoạt và chấm dứt hợp đồng; actorUserId lấy từ principal ADMIN.
public record ChangeContractStatusCommand(UUID actorUserId, UUID hotelId, UUID contractId) {}
