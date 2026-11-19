package com.team.hotelbooking.payment.application.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VnpayIpnResult(
    @JsonProperty("RspCode") String code, @JsonProperty("Message") String message) {
  public static VnpayIpnResult of(String code) {
    return new VnpayIpnResult(
        code,
        switch (code) {
          case "00" -> "Confirm Success";
          case "01" -> "Order not Found";
          case "02" -> "Order already confirmed";
          case "04" -> "Invalid Amount";
          case "97" -> "Invalid Signature";
          default -> "Unknown error";
        });
  }
}
