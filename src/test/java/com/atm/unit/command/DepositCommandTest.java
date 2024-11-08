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
  void execute_InvalidAmountLessThanOne_ThrowsException() {
    assertThrows(CommandException.class, () -> command.execute("0.00"));
    verifyNoInteractions(transactionService);
  }

  @Test
  void execute_NoLoggedInUser_ThrowsException() {

    when(sessionHolder.getCurrentSession()).thenReturn(null);

    assertThrows(CommandException.class, () -> command.execute("100.00"));
    verifyNoInteractions(transactionService);
  }

  @Test
  void execute_LoggedInUserExpiredSession_ThrowsException() {

    Session session = mock(Session.class);
    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(false);

    assertThrows(CommandException.class, () -> command.execute("100.00"));
    verifyNoInteractions(transactionService);
  }

  @Test
  void execute_InvalidAmount_ThrowsException() {

    Session session = mock(Session.class);
    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);

    CommandException exception =
        assertThrows(CommandException.class, () -> command.execute("invalid"));
    assertEquals("No active session, Please login first!", exception.getMessage());

    verifyNoInteractions(transactionService);
  }

  @Test
  void execute_ValidAmount_Success() {
    Session session = mock(Session.class);
    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(true);

    when(balanceService.getBalance(1L)).thenReturn(new BigDecimal("150.00"));

    command.execute("100.00");

    verify(transactionService).deposit(1L, new BigDecimal("100.00"));
    verify(balanceService).getBalance(1L);
  }
}
