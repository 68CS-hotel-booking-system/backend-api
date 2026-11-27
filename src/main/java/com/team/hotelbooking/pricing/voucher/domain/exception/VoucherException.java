package com.team.hotelbooking.pricing.voucher.domain.exception;

public class VoucherException extends RuntimeException {
  private final VoucherErrorCode code;

  public VoucherException(VoucherErrorCode code) {
    super(code.defaultMessage());
    this.code = code;
  }

  public VoucherErrorCode code() {
    return code;
  }
}
