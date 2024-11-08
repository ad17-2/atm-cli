package com.atm.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

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

  private static final Long TEST_USER_ID = 1L;
  private static final Long TARGET_USER_ID = 2L;

  @BeforeEach
  void setUp() {
    transactionService = new TransactionServiceImpl(database, balanceService);
  }

  @Test
  void deposit_amountLessThanOne_ThrowsException() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.deposit(TEST_USER_ID, new BigDecimal("0.5")));

    assertEquals("Invalid amount, must be grater than 1", exception.getMessage());
    verifyNoInteractions(database);
  }

  @Test
  void deposit_validAmount_Success() {
    when(balanceService.getBalance(TEST_USER_ID)).thenReturn(new BigDecimal("100.00"));

    transactionService.deposit(TEST_USER_ID, new BigDecimal("50.00"));

    verify(database).createTransaction(TEST_USER_ID, new BigDecimal("50.00"), "DEPOSIT");
  }

  @Test
  void withdraw_amountLessThanOne_ThrowsException() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.withdraw(TEST_USER_ID, new BigDecimal("0.5")));

    assertEquals("Invalid amount, must be grater than 1", exception.getMessage());
    verifyNoInteractions(database);
  }

  @Test
  void withdraw_insufficientBalance_ThrowsException() {
    when(balanceService.getBalance(TEST_USER_ID)).thenReturn(new BigDecimal("40.00"));

    InsufficientFundsException exception =
        assertThrows(
            InsufficientFundsException.class,
            () -> transactionService.withdraw(TEST_USER_ID, new BigDecimal("50.00")));

    assertEquals("Insufficient funds for withdrawal", exception.getMessage());
    verify(balanceService).getBalance(TEST_USER_ID);
    verifyNoInteractions(database);
  }

  @Test
  void withdraw_validAmount_Success() {
    when(balanceService.getBalance(TEST_USER_ID)).thenReturn(new BigDecimal("100.00"));

    transactionService.withdraw(TEST_USER_ID, new BigDecimal("50.00"));

    verify(database).createTransaction(TEST_USER_ID, new BigDecimal("50.00"), "WITHDRAW");
  }

  @Test
  void transfer_amountLessThanOne_ThrowsException() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.transfer(TEST_USER_ID, TARGET_USER_ID, new BigDecimal("0.5")));

    assertEquals("Invalid amount, must be grater than 1", exception.getMessage());
    verifyNoInteractions(database);
  }

  @Test
  void transfer_negativeAmount_ThrowsException() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                transactionService.transfer(
                    TEST_USER_ID, TARGET_USER_ID, new BigDecimal("-50.00")));

    assertEquals("Transfer amount must be positive", exception.getMessage());
    verifyNoInteractions(database);
  }

  @Test
  void transfer_sameAccount_ThrowsException() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> transactionService.transfer(TEST_USER_ID, TEST_USER_ID, new BigDecimal("50.00")));

    assertEquals("Cannot transfer to same account", exception.getMessage());
    verifyNoInteractions(database);
  }

  @Test
  void transfer_insufficientBalance_ThrowsException() {
    when(balanceService.getBalance(TEST_USER_ID)).thenReturn(new BigDecimal("40.00"));

    InsufficientFundsException exception =
        assertThrows(
            InsufficientFundsException.class,
            () ->
                transactionService.transfer(TEST_USER_ID, TARGET_USER_ID, new BigDecimal("50.00")));

    assertEquals("Insufficient funds for transfer", exception.getMessage());
    verify(balanceService).getBalance(TEST_USER_ID);
    verifyNoInteractions(database);
  }

  @Test
  void transfer_validAmount_Success() {
    when(balanceService.getBalance(TEST_USER_ID)).thenReturn(new BigDecimal("100.00"));

    transactionService.transfer(TEST_USER_ID, TARGET_USER_ID, new BigDecimal("50.00"));

    verify(database).performTransfer(TEST_USER_ID, TARGET_USER_ID, new BigDecimal("50.00"));
  }
}
