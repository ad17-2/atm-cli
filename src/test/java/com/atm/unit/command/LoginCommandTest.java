package com.atm.unit.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.atm.command.Command;
import com.atm.command.LoginCommand;
import com.atm.command.SessionHolder;
import com.atm.exception.ActiveSessionException;
import com.atm.exception.CommandException;
import com.atm.model.Session;
import com.atm.service.user.UserService;
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
class LoginCommandTest {

  @Mock private UserService userService;
  @Mock private SessionHolder sessionHolder;

  private Command command;

  @BeforeEach
  void setUp() {
    command = new LoginCommand(userService, sessionHolder);
  }

  @Test
  void execute_InvalidCredentials_ThrowsException() {

    when(userService.login(any(), any())).thenReturn(Optional.empty());

    assertThrows(CommandException.class, () -> command.execute("wronguser", "wrongpass"));
    assertNull(sessionHolder.getCurrentSession());
  }

  @Test
  void execute_whenRetryLoginWithinSameSession_ThrowsException() {
    when(sessionHolder.getCurrentSession()).thenReturn(Session.builder().userId(1L).build());
    CommandException exception =
        assertThrows(CommandException.class, () -> command.execute("testuser", "testpassword"));
    assertEquals("User already logged in. Please logout first.", exception.getMessage());
  }

  @Test
  void execute_ActiveSession_ThrowsException() {

    when(userService.login(any(), any()))
        .thenThrow(new ActiveSessionException("Already logged in"));

    assertThrows(CommandException.class, () -> command.execute("testuser", "password"));
    assertNull(sessionHolder.getCurrentSession());
  }

  @Test
  void execute_InvalidArgumentCount_ThrowsException() {

    assertThrows(CommandException.class, () -> command.execute("username"));
    assertThrows(CommandException.class, () -> command.execute("username", "password", "extra"));
    verifyNoInteractions(userService);
    assertNull(sessionHolder.getCurrentSession());
  }

  @Test
  void execute_ValidCredentials_Success() {

    String username = "testuser";
    String password = "Password123";
    Session mockSession = Session.builder().id(1L).userId(1L).build();

    when(userService.login(username, password)).thenReturn(Optional.of(mockSession));
    when(sessionHolder.getCurrentSession()).thenReturn(null);

    command.execute(username, password);

    assertNull(sessionHolder.getCurrentSession());
    verify(userService).login(username, password);
  }
}
