package com.atm.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.atm.database.Database;
import com.atm.exception.DatabaseException;
import com.atm.service.balance.BalanceService;
import com.atm.service.balance.BalanceServiceImpl;
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
class BalanceServiceImplTest {

  private static final Long TEST_USER_ID = 123L;

  @Mock private Database database;

  private BalanceService balanceService;

  @BeforeEach
  void setUp() {
    balanceService = new BalanceServiceImpl(database);
  }

  @Test
  void getBalance_ShouldReturnBalanceFromDatabase() {
    BigDecimal expectedBalance = new BigDecimal("1000.00");
    when(database.getBalance(TEST_USER_ID)).thenReturn(expectedBalance);

    BigDecimal actualBalance = balanceService.getBalance(TEST_USER_ID);

    assertEquals(expectedBalance, actualBalance);
    verify(database).getBalance(TEST_USER_ID);
  }

  @Test
  void getBalance_WhenDatabaseReturnsZero_ShouldReturnZero() {
    BigDecimal expectedBalance = BigDecimal.ZERO;
    when(database.getBalance(TEST_USER_ID)).thenReturn(expectedBalance);

    BigDecimal actualBalance = balanceService.getBalance(TEST_USER_ID);

    assertEquals(expectedBalance, actualBalance);
    verify(database).getBalance(TEST_USER_ID);
  }

  @Test
  void getBalance_WhenBalanceNotFound_ShouldThrowDatabaseException() {
    when(database.getBalance(TEST_USER_ID))
        .thenThrow(new DatabaseException("No balance record found for user: " + TEST_USER_ID));

    DatabaseException exception =
        assertThrows(DatabaseException.class, () -> balanceService.getBalance(TEST_USER_ID));
    assertEquals("No balance record found for user: " + TEST_USER_ID, exception.getMessage());
    verify(database).getBalance(TEST_USER_ID);
  }

  @Test
  void getBalance_WhenDatabaseFails_ShouldThrowDatabaseException() {
    when(database.getBalance(TEST_USER_ID))
        .thenThrow(new DatabaseException("Failed to get balance"));

    DatabaseException exception =
        assertThrows(DatabaseException.class, () -> balanceService.getBalance(TEST_USER_ID));
    assertEquals("Failed to get balance", exception.getMessage());
    verify(database).getBalance(TEST_USER_ID);
  }

  @Test
  void getBalance_WithNullUserId_ShouldThrowDatabaseException() {
    when(database.getBalance(null)).thenThrow(new DatabaseException("Failed to get balance"));

    DatabaseException exception =
        assertThrows(DatabaseException.class, () -> balanceService.getBalance(null));
    assertEquals("Failed to get balance", exception.getMessage());
    verify(database).getBalance(null);
  }
}
