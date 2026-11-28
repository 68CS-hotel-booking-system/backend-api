package com.team.hotelbooking.pricing.voucher.application.port;

import java.util.UUID;

public interface VoucherDiscountPort {
  long discount(UUID hotel, String code, long roomAmount);
}
