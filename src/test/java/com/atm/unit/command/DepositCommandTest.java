package com.atm.unit.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.atm.command.Command;
import com.atm.command.DepositCommand;
import com.atm.command.SessionHolder;
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
class DepositCommandTest {
  @Mock private TransactionService transactionService;
  @Mock private BalanceService balanceService;
  @Mock private SessionService sessionService;
  @Mock private SessionHolder sessionHolder;

  private Command command;

  private static final Long TEST_USER_ID = 1L;

  @BeforeEach
  void setUp() {
    command = new DepositCommand(transactionService, balanceService, sessionHolder, sessionService);
  }

  @Test
  void execute_hasNoActiveSession_ThrowsException() {

    when(sessionHolder.getCurrentSession()).thenReturn(null);

    CommandException exception =
        assertThrows(CommandException.class, () -> command.execute("1.00"));
    assertEquals("No active session, Please login first!", exception.getMessage());
    verifyNoInteractions(transactionService);
  }

  @Test
  void execute_hasExpiredSession_ThrowsException() {

    Session session = mock(Session.class);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(false);

    CommandException exception =
        assertThrows(CommandException.class, () -> command.execute("1.00"));
    assertEquals("No active session, Please login first!", exception.getMessage());
    verifyNoInteractions(transactionService);
  }

  @Test
  void execute_hasActiveSession_invalidAmountFormat_ThrowsException() {

    Session session = mock(Session.class);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(true);

    CommandException exception =
        assertThrows(CommandException.class, () -> command.execute("-asd"));
    assertEquals("Invalid amount format", exception.getMessage());
    verifyNoInteractions(transactionService);
  }

  @Test
  void execute_hasActiveSession_validAmountFormat_Success() {
    Session session = mock(Session.class);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(true);

    when(balanceService.getBalance(TEST_USER_ID)).thenReturn(BigDecimal.valueOf(1000));

    command.execute("100");

    verify(transactionService).deposit(TEST_USER_ID, BigDecimal.valueOf(100));
    verify(balanceService).getBalance(TEST_USER_ID);
  }
}
