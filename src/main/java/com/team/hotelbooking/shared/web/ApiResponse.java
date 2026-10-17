package com.team.hotelbooking.shared.web;

/** Success response envelope. Errors are returned separately as ProblemDetail. */
public record ApiResponse<T>(String message, T data) {

  public static <T> ApiResponse<T> success(T data) {
    return success("Thành công.", data);
  }

  public static <T> ApiResponse<T> success(String message, T data) {
    return new ApiResponse<>(message, data);
  }
}
