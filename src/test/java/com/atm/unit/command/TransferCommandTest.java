package com.atm.unit.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.atm.command.Command;
import com.atm.command.SessionHolder;
import com.atm.command.TransferCommand;
import com.atm.exception.CommandException;
import com.atm.model.Session;
import com.atm.model.User;
import com.atm.service.session.SessionService;
import com.atm.service.transaction.TransactionService;
import com.atm.service.user.UserService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransferCommandTest {

  @Mock private TransactionService transactionService;
  @Mock private UserService userService;
  @Mock private SessionService sessionService;
  @Mock private SessionHolder sessionHolder;

  private Command command;

  private static final Long TEST_USER_ID = 1L;
  private static final Long TARGET_USER_ID = 2L;
  private static final String TARGET_USERNAME = "targetUser";

  @BeforeEach
  void setUp() {
    command = new TransferCommand(userService, transactionService, sessionHolder, sessionService);
  }

  @Test
  void execute_hasNoActiveSession_ThrowsException() {
    when(sessionHolder.getCurrentSession()).thenReturn(null);

    CommandException exception =
        assertThrows(CommandException.class, () -> command.execute(TARGET_USERNAME, "100.00"));
    assertEquals("No active session, Please login first!", exception.getMessage());
    verifyNoInteractions(transactionService);
    verifyNoInteractions(userService);
  }

  @Test
  void execute_hasExpiredSession_ThrowsException() {
    Session session = mock(Session.class);
    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(false);

    CommandException exception =
        assertThrows(CommandException.class, () -> command.execute(TARGET_USERNAME, "100.00"));
    assertEquals("No active session, Please login first!", exception.getMessage());
    verify(sessionHolder).terminateSession();
    verifyNoInteractions(transactionService);
    verifyNoInteractions(userService);
  }

  @Test
  void execute_hasActiveSession_invalidAmountFormat_ThrowsException() {
    Session session = mock(Session.class);
    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(true);

    CommandException exception =
        assertThrows(CommandException.class, () -> command.execute(TARGET_USERNAME, "invalid"));
    assertEquals("Invalid amount format", exception.getMessage());
    verifyNoInteractions(transactionService);
    verifyNoInteractions(userService);
  }

  @Test
  void execute_hasActiveSession_userNotFound_ThrowsException() {
    Session session = mock(Session.class);
    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(true);
    when(userService.getUserByUsername(TARGET_USERNAME)).thenReturn(Optional.empty());

    CommandException exception =
        assertThrows(CommandException.class, () -> command.execute(TARGET_USERNAME, "100.00"));
    assertEquals("User not found", exception.getMessage());
    verify(userService).getUserByUsername(TARGET_USERNAME);
    verifyNoInteractions(transactionService);
  }

  @Test
  void execute_hasActiveSession_validAmountFormat_Success() {
    Session session = mock(Session.class);
    User targetUser = mock(User.class);

    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(true);
    when(userService.getUserByUsername(TARGET_USERNAME)).thenReturn(Optional.of(targetUser));
    when(targetUser.getId()).thenReturn(TARGET_USER_ID);

    command.execute(TARGET_USERNAME, "100");

    verify(transactionService).transfer(TEST_USER_ID, TARGET_USER_ID, new BigDecimal("100"));
  }

  @Test
  void execute_noArguments_ThrowsException() {
    CommandException exception = assertThrows(CommandException.class, () -> command.execute());
    assertEquals("Usage: transfer <username> <amount>", exception.getMessage());
    verifyNoInteractions(transactionService);
    verifyNoInteractions(userService);
    verifyNoInteractions(sessionService);
  }

  @Test
  void execute_insufficientArguments_ThrowsException() {
    CommandException exception =
        assertThrows(CommandException.class, () -> command.execute(TARGET_USERNAME));
    assertEquals("Usage: transfer <username> <amount>", exception.getMessage());
    verifyNoInteractions(transactionService);
    verifyNoInteractions(userService);
    verifyNoInteractions(sessionService);
  }

  @Test
  void execute_hasActiveSession_transferFails_ThrowsException() {
    Session session = mock(Session.class);
    User targetUser = mock(User.class);

    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(true);
    when(userService.getUserByUsername(TARGET_USERNAME)).thenReturn(Optional.of(targetUser));
    when(targetUser.getId()).thenReturn(TARGET_USER_ID);
    doThrow(new RuntimeException("Transfer failed"))
        .when(transactionService)
        .transfer(TEST_USER_ID, TARGET_USER_ID, new BigDecimal("100"));

    CommandException exception =
        assertThrows(CommandException.class, () -> command.execute(TARGET_USERNAME, "100"));
    assertEquals("Failed to transfer money", exception.getMessage());
    verify(transactionService).transfer(TEST_USER_ID, TARGET_USER_ID, new BigDecimal("100"));
  }
}
