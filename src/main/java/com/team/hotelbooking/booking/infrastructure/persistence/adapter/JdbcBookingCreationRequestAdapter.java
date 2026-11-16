package com.team.hotelbooking.booking.infrastructure.persistence.adapter;

import com.team.hotelbooking.booking.application.command.CreateBookingCommand;
import com.team.hotelbooking.booking.application.port.BookingCreationRequestPort;
import com.team.hotelbooking.booking.domain.exception.BookingErrorCode;
import com.team.hotelbooking.booking.domain.exception.BookingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
@RequiredArgsConstructor
public class JdbcBookingCreationRequestAdapter implements BookingCreationRequestPort {

  private final JdbcClient jdbc;
  private final JsonMapper jsonMapper;

  @Override
  public UUID claim(CreateBookingCommand command, UUID proposedBookingId) {
    String hash = hash(command);
    int inserted =
        jdbc.sql(
                """
        INSERT INTO booking_creation_requests (customer_id, request_key, request_hash, booking_id)
        VALUES (:customer, :key, :hash, :booking)
        ON CONFLICT (customer_id, request_key) DO NOTHING
        """)
            .param("customer", command.customerId())
            .param("key", command.idempotencyKey())
            .param("hash", hash)
            .param("booking", proposedBookingId)
            .update();
    if (inserted == 1) {
      return proposedBookingId;
    }
    return jdbc.sql(
            """
        SELECT booking_id, request_hash FROM booking_creation_requests
        WHERE customer_id = :customer AND request_key = :key
        """)
        .param("customer", command.customerId())
        .param("key", command.idempotencyKey())
        .query(
            (row, index) -> {
              if (!hash.equals(row.getString("request_hash"))) {
                throw new BookingException(BookingErrorCode.BOOKING_REQUEST_CONFLICT);
              }
              return row.getObject("booking_id", UUID.class);
            })
        .single();
  }

  private String hash(CreateBookingCommand command) {
    try {
      byte[] bytes = jsonMapper.writeValueAsBytes(command);
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
