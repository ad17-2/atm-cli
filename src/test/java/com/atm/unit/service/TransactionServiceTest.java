package com.atm.unit.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atm.database.Database;
import com.atm.exception.InsufficientFundsException;
import com.atm.service.balance.BalanceService;
import com.atm.service.transaction.TransactionService;
import com.atm.service.transaction.TransactionServiceImpl;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionServiceTest {

  @Mock private Database database;
  @Mock private BalanceService balanceService;

  private TransactionService transactionService;

  @BeforeEach
  void setUp() {
    transactionService = new TransactionServiceImpl(database, balanceService);
  }

  @Test
  void deposit_ValidAmount_Success() {

    Long userId = 1L;
    BigDecimal initialBalance = new BigDecimal("100.00");
    BigDecimal depositAmount = new BigDecimal("50.00");

    when(balanceService.getBalance(userId)).thenReturn(initialBalance);

    transactionService.deposit(userId, depositAmount);

    verify(database).createTransaction(userId, depositAmount, "DEPOSIT");
  }

  @Test
  void withdraw_ValidAmount_Success() {

    Long userId = 1L;
    BigDecimal initialBalance = new BigDecimal("100.00");
    BigDecimal withdrawAmount = new BigDecimal("50.00");

    when(balanceService.getBalance(userId)).thenReturn(initialBalance);

    transactionService.withdraw(userId, withdrawAmount);

    verify(database).createTransaction(userId, withdrawAmount, "WITHDRAW");
  }

  @Test
  void withdraw_InsufficientFunds_ThrowsException() {

    Long userId = 1L;
    BigDecimal initialBalance = new BigDecimal("40.00");
    BigDecimal withdrawAmount = new BigDecimal("50.00");

    when(balanceService.getBalance(userId)).thenReturn(initialBalance);

    assertThrows(
        InsufficientFundsException.class,
        () -> transactionService.withdraw(userId, withdrawAmount));

    verify(database, never()).createTransaction(any(), any(), any());
  }

  @Test
  void transfer_ValidAmount_Success() {

    Long fromUserId = 1L;
    Long toUserId = 2L;
    BigDecimal fromBalance = new BigDecimal("100.00");
    BigDecimal toBalance = new BigDecimal("50.00");
    BigDecimal transferAmount = new BigDecimal("30.00");

    when(balanceService.getBalance(fromUserId)).thenReturn(fromBalance);
    when(balanceService.getBalance(toUserId)).thenReturn(toBalance);

    transactionService.transfer(fromUserId, toUserId, transferAmount);

    verify(database).performTransfer(fromUserId, toUserId, transferAmount);
  }

  @Test
  void transfer_InsufficientFunds_ThrowsException() {

    Long fromUserId = 1L;
    Long toUserId = 2L;
    BigDecimal fromBalance = new BigDecimal("20.00");
    BigDecimal transferAmount = new BigDecimal("30.00");

    when(balanceService.getBalance(fromUserId)).thenReturn(fromBalance);

    assertThrows(
        InsufficientFundsException.class,
        () -> transactionService.transfer(fromUserId, toUserId, transferAmount));

    verify(database, never()).performTransfer(any(), any(), any());
  }
}
