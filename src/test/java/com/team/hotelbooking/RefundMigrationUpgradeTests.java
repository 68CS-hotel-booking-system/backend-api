package com.team.hotelbooking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

class RefundMigrationUpgradeTests {
  @Test
  void upgradeShouldBackfillOnlyUnconfirmedReceiptsWithoutChangingOriginalPayments() {
    try (var postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17"))) {
      postgres.start();
      var datasource =
          new DriverManagerDataSource(
              postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
      Flyway.configure()
          .dataSource(datasource)
          .locations("classpath:db/migration")
          .target("20261124180000000")
          .load()
          .migrate();
      var jdbc = new JdbcTemplate(datasource);
      UUID user = UUID.randomUUID(),
          hotel = UUID.randomUUID(),
          booking = UUID.randomUUID(),
          late = UUID.randomUUID(),
          normal = UUID.randomUUID();
      jdbc.update(
          "INSERT INTO users(id,email,password_hash,full_name,phone,status,email_verified_at) VALUES (?,'upgrade@example.test','unused','Khách','+84912340000','active',now())",
          user);
      jdbc.update(
          "INSERT INTO hotels(id,owner_user_id,city_id,name,slug,address,status) VALUES (?,?,'822cafba-73f6-4ec0-bae7-71804535cae3','Upgrade','refund-upgrade','Đà Nẵng','ACTIVE')",
          hotel,
          user);
      jdbc.update(
          "INSERT INTO bookings(id,booking_code,customer_id,hotel_id,booker_name_snapshot,booker_email_snapshot,booker_phone_snapshot,guest_name_snapshot,guest_email_snapshot,guest_phone_snapshot,check_in,check_out,days,nights,status) VALUES (?,'UPGRADE',?,?,'Khách','upgrade@example.test','+84912340000','Khách','upgrade@example.test','+84912340000','2027-06-01','2027-06-03',3,2,'EXPIRED')",
          booking,
          user,
          hotel);
      for (UUID payment : java.util.List.of(late, normal))
        jdbc.update(
            "INSERT INTO payment_transactions(id,booking_id,customer_id,amount_vnd,status,payment_url,created_at,expires_at,booking_confirmed) VALUES (?,?,?,123456,'SUCCEEDED','https://example.test/pay',now()-interval '1 day',now()-interval '23 hour',?)",
            payment,
            booking,
            user,
            payment.equals(normal));
      var before = jdbc.queryForList("SELECT * FROM payment_transactions ORDER BY id");
      var flyway =
          Flyway.configure().dataSource(datasource).locations("classpath:db/migration").load();
      assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM payment_refunds", Long.class))
          .isEqualTo(1);
      assertThat(jdbc.queryForObject("SELECT payment_id FROM payment_refunds", UUID.class))
          .isEqualTo(late);
      assertThat(jdbc.queryForObject("SELECT amount FROM payment_refunds", Long.class))
          .isEqualTo(123456);
      assertThat(jdbc.queryForList("SELECT * FROM payment_transactions ORDER BY id"))
          .isEqualTo(before);
      assertThat(flyway.migrate().migrationsExecuted).isZero();
    }
  }
}
