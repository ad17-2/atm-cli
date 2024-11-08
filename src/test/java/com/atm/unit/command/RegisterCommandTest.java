package com.atm.unit.command;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.atm.command.RegisterCommand;
import com.atm.exception.CommandException;
import com.atm.model.User;
import com.atm.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegisterCommandTest {

  @Mock private UserService userService;

  private RegisterCommand command;

  @BeforeEach
  void setUp() {
    command = new RegisterCommand(userService);
  }

  @Test
  void execute_ValidArguments_Success() {
    String username = "testuser";
    String password = "Password123";
    when(userService.register(username, password))
        .thenReturn(User.builder().username(username).build());

    command.execute(username, password);

    verify(userService).register(username, password);
  }

  @Test
  void execute_InvalidArgumentCount_ThrowsException() {
    assertThrows(CommandException.class, () -> command.execute("username"));
    assertThrows(CommandException.class, () -> command.execute("username", "password", "extra"));
    verifyNoInteractions(userService);
  }

  @Test
  void execute_ServiceThrowsException_ThrowsCommandException() {
    when(userService.register(any(), any()))
        .thenThrow(new IllegalArgumentException("Invalid username"));

    CommandException exception =
        assertThrows(CommandException.class, () -> command.execute("user", "pass"));
    assertTrue(exception.getMessage().contains("Invalid username"));
  }
}
