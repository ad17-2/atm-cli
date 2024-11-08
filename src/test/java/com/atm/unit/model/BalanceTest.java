package com.atm.unit.model;

import static org.junit.jupiter.api.Assertions.*;

import com.atm.model.Balance;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class BalanceTest {

  @Test
  void whenBalanceCreatedWithValidData_thenSucceeds() {
    LocalDateTime now = LocalDateTime.now();
    BigDecimal amount = new BigDecimal("100.00");

    Balance balance = Balance.builder().userId(1L).balance(amount).lastUpdated(now).build();

    assertEquals(1L, balance.getUserId());
    assertEquals(amount, balance.getBalance());
    assertEquals(now, balance.getLastUpdated());
  }

  @Test
  void whenBalanceBuiltWithoutOptionalFields_thenSucceeds() {
    Balance balance = Balance.builder().userId(1L).balance(BigDecimal.ZERO).build();

    assertNotNull(balance.getLastUpdated());
    assertEquals(BigDecimal.ZERO, balance.getBalance());
  }
}
