package com.team.hotelbooking.shared.web;

/** Validation error without the rejected value, which may contain sensitive data. */
public record FieldErrorResponse(String field, String message) {}
