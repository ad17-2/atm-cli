package com.atm.unit.model;

import static org.junit.jupiter.api.Assertions.*;

import com.atm.model.Transaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TransactionTest {

  @Test
  void whenTransactionCreatedWithValidData_thenSucceeds() {
    LocalDateTime now = LocalDateTime.now();
    BigDecimal amount = new BigDecimal("50.00");

    Transaction transaction =
        Transaction.builder()
            .id(1L)
            .fromUserId(1L)
            .toUserId(2L)
            .amount(amount)
            .type(Transaction.TransactionType.TRANSFER)
            .createdAt(now)
            .build();

    assertEquals(1L, transaction.getId());
    assertEquals(1L, transaction.getFromUserId());
    assertEquals(2L, transaction.getToUserId());
    assertEquals(amount, transaction.getAmount());
    assertEquals(Transaction.TransactionType.TRANSFER, transaction.getType());
    assertEquals(now, transaction.getCreatedAt());
  }

  @Test
  void whenDepositTransaction_thenFromUserIdCanBeNull() {
    Transaction transaction =
        Transaction.builder()
            .id(1L)
            .toUserId(1L)
            .amount(new BigDecimal("100.00"))
            .type(Transaction.TransactionType.DEPOSIT)
            .build();

    assertNull(transaction.getFromUserId());
    assertNotNull(transaction.getCreatedAt());
  }

  @Test
  void whenWithdrawTransaction_thenToUserIdCanBeNull() {
    Transaction transaction =
        Transaction.builder()
            .id(1L)
            .fromUserId(1L)
            .amount(new BigDecimal("100.00"))
            .type(Transaction.TransactionType.WITHDRAW)
            .build();

    assertNull(transaction.getToUserId());
    assertNotNull(transaction.getCreatedAt());
  }
}
