package com.atm.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.atm.database.Database;
import com.atm.exception.ActiveSessionException;
import com.atm.model.Session;
import com.atm.model.User;
import com.atm.service.session.SessionService;
import com.atm.service.user.UserServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UserServiceTest {

  @Mock private Database database;

  @Mock private SessionService sessionService;

  private UserServiceImpl userService;

  private static final String USERNAME = "testUser";
  private static final String PASSWORD = "testPass123!";
  private static final Long USER_ID = 1L;
  private static final Long SESSION_ID = 100L;

  @BeforeEach
  void setUp() {
    userService = new UserServiceImpl(database, sessionService);
  }

  @Test
  void register_ValidCredentials_Success() {

    when(database.createUser(anyString(), anyString())).thenReturn(USER_ID);

    User result = userService.register(USERNAME, PASSWORD);

    assertNotNull(result);
    assertEquals(USERNAME, result.getUsername());
    assertTrue(BCrypt.checkpw(PASSWORD, result.getPasswordHash()));
    verify(database).createUser(eq(USERNAME), any());
  }

  @Test
  void register_InvalidUsername_ThrowsException() {
    assertThrows(
        IllegalArgumentException.class, () -> userService.register("a", PASSWORD)); // too short
  }

  @Test
  void register_InvalidPassword_ThrowsException() {
    assertThrows(
        IllegalArgumentException.class, () -> userService.register(USERNAME, "weak")); // too weak
  }

  @Test
  void login_ValidCredentials_Success() {

    String hashedPassword = BCrypt.hashpw(PASSWORD, BCrypt.gensalt());
    User mockUser =
        User.builder().id(USER_ID).username(USERNAME).passwordHash(hashedPassword).build();

    when(database.getUserByUsername(USERNAME)).thenReturn(Optional.of(mockUser));
    when(sessionService.hasActiveSession(USER_ID)).thenReturn(false);
    when(sessionService.createSession(USER_ID)).thenReturn(SESSION_ID);

    Optional<Session> result = userService.login(USERNAME, PASSWORD);

    assertTrue(result.isPresent());
    assertEquals(SESSION_ID, result.get().getId());
    assertEquals(USER_ID, result.get().getUserId());
    verify(database).updateLastLogin(USER_ID);
  }

  @Test
  void login_InvalidPassword_ReturnsEmpty() {

    User mockUser =
        User.builder()
            .id(USER_ID)
            .username(USERNAME)
            .passwordHash(BCrypt.hashpw("differentPass", BCrypt.gensalt()))
            .build();

    when(database.getUserByUsername(USERNAME)).thenReturn(Optional.of(mockUser));
    when(sessionService.hasActiveSession(USER_ID)).thenReturn(false);

    Optional<Session> result = userService.login(USERNAME, PASSWORD);

    assertTrue(result.isEmpty());
    verify(sessionService, never()).createSession(anyLong());
    verify(database, never()).updateLastLogin(anyLong());
  }

  @Test
  void login_UserNotFound_ReturnsEmpty() {

    when(database.getUserByUsername(USERNAME)).thenReturn(Optional.empty());

    Optional<Session> result = userService.login(USERNAME, PASSWORD);

    assertTrue(result.isEmpty());
    verify(sessionService, never()).createSession(anyLong());
    verify(database, never()).updateLastLogin(anyLong());
  }

  @Test
  void login_ActiveSessionExists_ThrowsException() {

    User mockUser =
        User.builder()
            .id(USER_ID)
            .username(USERNAME)
            .passwordHash(BCrypt.hashpw(PASSWORD, BCrypt.gensalt()))
            .build();

    when(database.getUserByUsername(USERNAME)).thenReturn(Optional.of(mockUser));
    when(sessionService.hasActiveSession(USER_ID)).thenReturn(true);

    assertThrows(ActiveSessionException.class, () -> userService.login(USERNAME, PASSWORD));
    verify(sessionService, never()).createSession(anyLong());
    verify(database, never()).updateLastLogin(anyLong());
  }

  @Test
  void logout_ValidSession_Success() {

    Session session = Session.builder().id(SESSION_ID).userId(USER_ID).build();

    userService.logout(session);

    verify(sessionService).terminateSession(SESSION_ID);
  }

  @Test
  void logout_NullSession_NoAction() {

    userService.logout(null);

    verify(sessionService, never()).terminateSession(anyLong());
  }
}
