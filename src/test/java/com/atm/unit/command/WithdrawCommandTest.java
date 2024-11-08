package com.atm.unit.command;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.atm.command.SessionHolder;
import com.atm.command.WithdrawCommand;
import com.atm.exception.CommandException;
import com.atm.model.Session;
import com.atm.service.balance.BalanceService;
import com.atm.service.session.SessionService;
import com.atm.service.transaction.TransactionService;
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
class WithdrawCommandTest {

  private static final Long TEST_USER_ID = 123L;
  private static final BigDecimal VALID_AMOUNT = new BigDecimal("100");

  @Mock private TransactionService transactionService;
  @Mock private BalanceService balanceService;
  @Mock private SessionHolder sessionHolder;
  @Mock private SessionService sessionService;

  private WithdrawCommand withdrawCommand;

  @BeforeEach
  void setUp() {
    withdrawCommand =
        new WithdrawCommand(transactionService, balanceService, sessionHolder, sessionService);
  }

  @Test
  void execute_WithValidAmountAndSufficientBalance_ShouldWithdrawSuccessfully() {
    Session session = mock(Session.class);
    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(true);
    when(balanceService.getBalance(TEST_USER_ID))
        .thenReturn(new BigDecimal("500"))
        .thenReturn(new BigDecimal("400"));

    withdrawCommand.execute("100");

    verify(sessionService).hasActiveSession(TEST_USER_ID);
    verify(transactionService).withdraw(TEST_USER_ID, VALID_AMOUNT);
    verify(balanceService, times(2)).getBalance(TEST_USER_ID);
  }

  @Test
  void execute_WithNoArguments_ShouldThrowCommandException() {
    CommandException exception =
        assertThrows(CommandException.class, () -> withdrawCommand.execute());
    assertEquals("Usage: withdraw <amount>", exception.getMessage());

    verifyNoInteractions(transactionService);
    verifyNoInteractions(sessionService);
  }

  @Test
  void execute_WithMultipleArguments_ShouldThrowCommandException() {
    CommandException exception =
        assertThrows(CommandException.class, () -> withdrawCommand.execute("100", "200"));
    assertEquals("Usage: withdraw <amount>", exception.getMessage());

    verifyNoInteractions(transactionService);
    verifyNoInteractions(sessionService);
  }

  @Test
  void execute_WithInvalidAmountFormat_ShouldThrowCommandException() {
    Session session = mock(Session.class);
    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(true);

    CommandException exception =
        assertThrows(CommandException.class, () -> withdrawCommand.execute("invalid"));
    assertEquals("Invalid amount format", exception.getMessage());

    verify(sessionService).hasActiveSession(TEST_USER_ID);
    verifyNoInteractions(transactionService);
  }

  @Test
  void execute_WithAmountLessThanOne_ShouldThrowCommandException() {
    Session session = mock(Session.class);
    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(true);

    CommandException exception =
        assertThrows(CommandException.class, () -> withdrawCommand.execute("0.5"));
    assertEquals("Invalid amount, must be grater than 1", exception.getMessage());

    verify(sessionService).hasActiveSession(TEST_USER_ID);
    verifyNoInteractions(transactionService);
  }

  @Test
  void execute_WithInsufficientBalance_ShouldThrowCommandException() {
    Session session = mock(Session.class);
    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(true);
    when(balanceService.getBalance(TEST_USER_ID)).thenReturn(new BigDecimal("50"));

    CommandException exception =
        assertThrows(CommandException.class, () -> withdrawCommand.execute("100"));
    assertEquals("Insufficient balance", exception.getMessage());

    verify(sessionService).hasActiveSession(TEST_USER_ID);
    verify(balanceService).getBalance(TEST_USER_ID);
    verify(transactionService, never()).withdraw(any(), any());
  }

  @Test
  void execute_WithNoActiveSession_ShouldThrowCommandException() {
    Session session = mock(Session.class);
    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(false);

    CommandException exception =
        assertThrows(CommandException.class, () -> withdrawCommand.execute("100"));
    assertEquals("No active session, Please login first!", exception.getMessage());

    verify(sessionService).hasActiveSession(TEST_USER_ID);
    verify(sessionHolder).terminateSession();
    verifyNoInteractions(transactionService);
  }

  @Test
  void execute_WhenSessionIsNull_ShouldThrowCommandException() {
    when(sessionHolder.getCurrentSession()).thenReturn(null);

    CommandException exception =
        assertThrows(CommandException.class, () -> withdrawCommand.execute("100"));
    assertEquals("No active session, Please login first!", exception.getMessage());

    verifyNoInteractions(sessionService);
    verifyNoInteractions(transactionService);
  }

  @Test
  void execute_WhenTransactionServiceThrowsException_ShouldThrowCommandException() {
    Session session = mock(Session.class);
    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(true);
    when(balanceService.getBalance(TEST_USER_ID)).thenReturn(new BigDecimal("500"));
    doThrow(new RuntimeException("Transaction failed"))
        .when(transactionService)
        .withdraw(TEST_USER_ID, VALID_AMOUNT);

    CommandException exception =
        assertThrows(CommandException.class, () -> withdrawCommand.execute("100"));
    assertEquals("Failed to withdraw money", exception.getMessage());

    verify(sessionService).hasActiveSession(TEST_USER_ID);
    verify(balanceService).getBalance(TEST_USER_ID);
    verify(transactionService).withdraw(TEST_USER_ID, VALID_AMOUNT);
  }
}
