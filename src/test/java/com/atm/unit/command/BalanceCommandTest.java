package com.atm.unit.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.atm.command.BalanceCommand;
import com.atm.command.Command;
import com.atm.command.SessionHolder;
import com.atm.exception.CommandException;
import com.atm.model.Session;
import com.atm.service.balance.BalanceService;
import com.atm.service.session.SessionService;
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
class BalanceCommandTest {
  @Mock private BalanceService balanceService;
  @Mock private SessionHolder sessionHolder;
  @Mock private SessionService sessionService;

  private Command command;

  private static final Long TEST_USER_ID = 1L;

  @BeforeEach
  void setUp() {
    command = new BalanceCommand(balanceService, sessionHolder, sessionService);
  }

  @Test
  void execute_NoLoggedInUser_ThrowsException() {

    when(sessionHolder.getCurrentSession()).thenReturn(null);

    CommandException exception =
        assertThrows(CommandException.class, () -> command.execute("balance"));
    assertEquals("No active session, Please login first!", exception.getMessage());
    verifyNoInteractions(balanceService);
  }

  @Test
  void execute_LoggedInUserExpiredSession_ThrowsException() {

    Session session = mock(Session.class);
    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(false);

    CommandException exception =
        assertThrows(CommandException.class, () -> command.execute("balance"));
    assertEquals("No active session, Please login first!", exception.getMessage());
    verifyNoInteractions(balanceService);
  }

  @Test
  void execute_LoggedInUser_ShowBalance() {

    Session session = mock(Session.class);
    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(true);
    when(balanceService.getBalance(TEST_USER_ID)).thenReturn(new BigDecimal(10L));

    command.execute("balance");

    verify(balanceService, times(1)).getBalance(TEST_USER_ID);
  }
}
