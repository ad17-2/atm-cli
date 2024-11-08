package com.atm.unit.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

  private static final Long TEST_USER_ID = 123L;
  private static final Long TARGET_USER_ID = 456L;
  private static final String TARGET_USERNAME = "targetUser";
  private static final BigDecimal VALID_AMOUNT = new BigDecimal("100");

  @Mock private TransactionService transactionService;
  @Mock private UserService userService;
  @Mock private SessionHolder sessionHolder;
  @Mock private SessionService sessionService;

  private TransferCommand transferCommand;

  @BeforeEach
  void setUp() {
    transferCommand =
        new TransferCommand(userService, transactionService, sessionHolder, sessionService);
  }

  @Test
  void execute_WithValidAmountAndSufficientBalance_ShouldTransferSuccessfully() {
    Session session = mock(Session.class);
    User targetUser = mock(User.class);

    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(true);
    when(userService.getUserByUsername(TARGET_USERNAME)).thenReturn(Optional.of(targetUser));
    when(targetUser.getId()).thenReturn(TARGET_USER_ID);

    transferCommand.execute(TARGET_USERNAME, "100");

    verify(sessionService).hasActiveSession(TEST_USER_ID);
    verify(transactionService).transfer(TEST_USER_ID, TARGET_USER_ID, VALID_AMOUNT);
  }

  @Test
  void execute_WithNoArguments_ShouldThrowCommandException() {
    CommandException exception =
        assertThrows(CommandException.class, () -> transferCommand.execute());
    assertEquals("Usage: transfer <username> <amount>", exception.getMessage());

    verifyNoInteractions(transactionService);
    verifyNoInteractions(userService);
    verifyNoInteractions(sessionService);
  }

  @Test
  void execute_WithInvalidArgumentCount_ShouldThrowCommandException() {
    CommandException exception =
        assertThrows(CommandException.class, () -> transferCommand.execute(TARGET_USERNAME));
    assertEquals("Usage: transfer <username> <amount>", exception.getMessage());

    verifyNoInteractions(transactionService);
    verifyNoInteractions(userService);
    verifyNoInteractions(sessionService);
  }

  @Test
  void execute_WithInvalidAmountFormat_ShouldThrowCommandException() {
    Session session = mock(Session.class);
    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(true);

    CommandException exception =
        assertThrows(
            CommandException.class, () -> transferCommand.execute(TARGET_USERNAME, "invalid"));
    assertEquals("Invalid amount format", exception.getMessage());

    verify(sessionService).hasActiveSession(TEST_USER_ID);
    verifyNoInteractions(transactionService);
    verifyNoInteractions(userService);
  }

  @Test
  void execute_WithInsufficientBalance_ShouldThrowCommandException() {
    Session session = mock(Session.class);
    User targetUser = mock(User.class);

    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(true);
    when(userService.getUserByUsername(TARGET_USERNAME)).thenReturn(Optional.of(targetUser));

    CommandException exception =
        assertThrows(CommandException.class, () -> transferCommand.execute(TARGET_USERNAME, "100"));
    assertEquals("Insufficient balance", exception.getMessage());

    verify(sessionService).hasActiveSession(TEST_USER_ID);
    verify(transactionService, never()).transfer(any(), any(), any());
  }

  @Test
  void execute_WithNoActiveSession_ShouldThrowCommandException() {
    Session session = mock(Session.class);
    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(false);

    CommandException exception =
        assertThrows(CommandException.class, () -> transferCommand.execute(TARGET_USERNAME, "100"));
    assertEquals("No active session, Please login first!", exception.getMessage());

    verify(sessionService).hasActiveSession(TEST_USER_ID);
    verify(sessionHolder).terminateSession();
    verifyNoInteractions(transactionService);
    verifyNoInteractions(userService);
  }

  @Test
  void execute_WhenSessionIsNull_ShouldThrowCommandException() {
    when(sessionHolder.getCurrentSession()).thenReturn(null);

    CommandException exception =
        assertThrows(CommandException.class, () -> transferCommand.execute(TARGET_USERNAME, "100"));
    assertEquals("No active session, Please login first!", exception.getMessage());

    verifyNoInteractions(sessionService);
    verifyNoInteractions(transactionService);
    verifyNoInteractions(userService);
  }

  @Test
  void execute_WhenUserNotFound_ShouldThrowCommandException() {
    Session session = mock(Session.class);
    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(true);
    when(userService.getUserByUsername(TARGET_USERNAME)).thenReturn(Optional.empty());

    CommandException exception =
        assertThrows(CommandException.class, () -> transferCommand.execute(TARGET_USERNAME, "100"));
    assertEquals("User not found", exception.getMessage());

    verify(sessionService).hasActiveSession(TEST_USER_ID);
    verify(userService).getUserByUsername(TARGET_USERNAME);
    verifyNoInteractions(transactionService);
  }

  @Test
  void execute_WhenTransactionServiceThrowsException_ShouldThrowCommandException() {
    Session session = mock(Session.class);
    User targetUser = mock(User.class);

    when(session.getUserId()).thenReturn(TEST_USER_ID);
    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(true);
    when(userService.getUserByUsername(TARGET_USERNAME)).thenReturn(Optional.of(targetUser));
    when(targetUser.getId()).thenReturn(TARGET_USER_ID);
    doThrow(new RuntimeException("Transaction failed"))
        .when(transactionService)
        .transfer(TEST_USER_ID, TARGET_USER_ID, VALID_AMOUNT);

    CommandException exception =
        assertThrows(CommandException.class, () -> transferCommand.execute(TARGET_USERNAME, "100"));
    assertEquals("Failed to transfer money", exception.getMessage());

    verify(sessionService).hasActiveSession(TEST_USER_ID);
    verify(transactionService).transfer(TEST_USER_ID, TARGET_USER_ID, VALID_AMOUNT);
  }
}
