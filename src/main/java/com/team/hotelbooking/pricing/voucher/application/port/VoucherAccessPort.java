package com.team.hotelbooking.pricing.voucher.application.port;

import java.util.UUID;

public interface VoucherAccessPort {
  boolean isCurrentOwner(UUID actor, UUID hotel);
}
