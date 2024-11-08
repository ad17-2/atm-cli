package com.atm.unit.command;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.atm.command.LogoutCommand;
import com.atm.command.SessionHolder;
import com.atm.exception.CommandException;
import com.atm.model.Session;
import com.atm.service.session.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LogoutCommandTest {

  private static final String TEST_USERNAME = "testUser";
  private static final Long TEST_USER_ID = 123L;
  private static final Long TEST_SESSION_ID = 456L;

  @Mock private SessionHolder sessionHolder;

  @Mock private SessionService sessionService;

  private LogoutCommand logoutCommand;

  @BeforeEach
  void setUp() {
    logoutCommand = new LogoutCommand(sessionService, sessionHolder);
  }

  @Test
  void execute_WithValidSession_ShouldLogoutSuccessfully() {
    Session session =
        Session.builder().id(TEST_SESSION_ID).userId(TEST_USER_ID).username(TEST_USERNAME).build();

    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(true);

    logoutCommand.execute();

    verify(sessionHolder).getCurrentSession();
    verify(sessionService).hasActiveSession(TEST_USER_ID);
    verify(sessionHolder).terminateSession();
    verify(sessionService).terminateSession(TEST_SESSION_ID);
  }

  @Test
  void execute_WhenNoCurrentSession_ShouldThrowCommandException() {

    when(sessionHolder.getCurrentSession()).thenReturn(null);

    CommandException exception =
        assertThrows(CommandException.class, () -> logoutCommand.execute());
    assertEquals("No active session, Please login first!", exception.getMessage());

    verify(sessionHolder).getCurrentSession();
    verifyNoInteractions(sessionService);
  }

  @Test
  void execute_WhenNoActiveSessionInService_ShouldThrowCommandException() {

    Session session =
        Session.builder().id(TEST_SESSION_ID).userId(TEST_USER_ID).username(TEST_USERNAME).build();

    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(false);

    CommandException exception =
        assertThrows(CommandException.class, () -> logoutCommand.execute());
    assertEquals("No active session, Please login first!", exception.getMessage());

    verify(sessionHolder).getCurrentSession();
    verify(sessionService).hasActiveSession(TEST_USER_ID);
    verify(sessionHolder).terminateSession();
  }

  @Test
  void execute_WhenSessionServiceThrowsException_ShouldThrowCommandException() {

    Session session =
        Session.builder().id(TEST_SESSION_ID).userId(TEST_USER_ID).username(TEST_USERNAME).build();

    when(sessionHolder.getCurrentSession()).thenReturn(session);
    when(sessionService.hasActiveSession(TEST_USER_ID)).thenReturn(true);
    doThrow(new RuntimeException("Database error"))
        .when(sessionService)
        .terminateSession(TEST_SESSION_ID);

    CommandException exception =
        assertThrows(CommandException.class, () -> logoutCommand.execute());
    assertEquals("Failed to logout", exception.getMessage());

    verify(sessionHolder).getCurrentSession();
    verify(sessionService).hasActiveSession(TEST_USER_ID);
    verify(sessionService).terminateSession(TEST_SESSION_ID);
  }
}
