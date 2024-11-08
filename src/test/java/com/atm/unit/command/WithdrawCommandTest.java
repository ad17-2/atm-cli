package com.atm.unit.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.atm.command.Command;
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

  @Mock private TransactionService transactionService;
  @Mock private BalanceService balanceService;
  @Mock private SessionService sessionService;
  @Mock private SessionHolder sessionHolder;

  private Command command;

  private static final Long TEST_USER_ID = 1L;

  @BeforeEach
  void setUp() {
    command =
        new WithdrawCommand(transactionService, balanceService, sessionHolder, sessionService);
  }

  @Test
  void execute_hasNoActiveSession_ThrowsException() {
    when(sessionHolder.getCurrentSession()).thenReturn(null);

    CommandException exception =
        assertThrows(CommandException.class, () -> command.execute("100.00"));
    assertEquals("No active session, Please login first!", exception.getMessage());
    verifyNoInteractions(transactionService);
  }

  @Test
  void execute_hasExpiredSession_ThrowsException() {
    Session session = mock(Session.class);
    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(false);

    CommandException exception =
        assertThrows(CommandException.class, () -> command.execute("100.00"));
    assertEquals("No active session, Please login first!", exception.getMessage());
    verify(sessionHolder).terminateSession();
    verifyNoInteractions(transactionService);
  }

  @Test
  void execute_hasActiveSession_invalidAmountFormat_ThrowsException() {
    Session session = mock(Session.class);
    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(true);

    CommandException exception =
        assertThrows(CommandException.class, () -> command.execute("invalid"));
    assertEquals("Invalid amount format", exception.getMessage());
    verifyNoInteractions(transactionService);
  }

  @Test
  void execute_hasActiveSession_validAmountFormat_Success() {
    Session session = mock(Session.class);
    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(true);
    when(balanceService.getBalance(TEST_USER_ID)).thenReturn(new BigDecimal("500.00"));

    command.execute("100");

    verify(transactionService).withdraw(TEST_USER_ID, new BigDecimal("100"));
    verify(balanceService, times(1)).getBalance(TEST_USER_ID);
  }

  @Test
  void execute_noArguments_ThrowsException() {
    CommandException exception = assertThrows(CommandException.class, () -> command.execute());
    assertEquals("Usage: withdraw <amount>", exception.getMessage());
    verifyNoInteractions(transactionService);
    verifyNoInteractions(sessionService);
  }
}
