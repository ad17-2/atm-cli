package com.atm.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.atm.database.Database;
import com.atm.model.Session;
import com.atm.service.session.SessionService;
import com.atm.service.session.SessionServiceImpl;
import java.time.LocalDateTime;
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
class SessionServiceTest {

  @Mock private Database database;

  private SessionService sessionService;

  @BeforeEach
  void setUp() {
    sessionService = new SessionServiceImpl(database);
  }

  @Test
  void createSession_Success() {
    Long userId = 1L;
    Long expectedSessionId = 1L;
    when(database.createSession(userId)).thenReturn(expectedSessionId);

    Long sessionId = sessionService.createSession(userId);

    assertEquals(expectedSessionId, sessionId);
    verify(database).createSession(userId);
  }

  @Test
  void terminateSession_Success() {
    Long sessionId = 1L;
    doNothing().when(database).deleteSession(sessionId);

    sessionService.terminateSession(sessionId);

    verify(database).deleteSession(sessionId);
  }

  @Test
  void validateSession_NullSessionId_ReturnsEmpty() {
    Optional<Session> result = sessionService.validateSession(null);

    assertFalse(result.isPresent());
    verify(database, never()).getSessionById(anyLong());
  }

  @Test
  void validateSession_ExpiredSession_TerminatesAndReturnsEmpty() {
    Long sessionId = 1L;
    Session expiredSession =
        Session.builder()
            .id(sessionId)
            .userId(1L)
            .lastActivityAt(LocalDateTime.now().minusHours(1))
            .build();
    when(database.getSessionById(sessionId)).thenReturn(Optional.of(expiredSession));

    Optional<Session> result = sessionService.validateSession(sessionId);

    assertFalse(result.isPresent());
    verify(database).deleteSession(sessionId);
    verify(database, never()).updateSessionActivity(anyLong());
  }

  @Test
  void validateSession_ValidSession_UpdatesActivityAndReturnsSession() {
    Long sessionId = 1L;
    Session validSession =
        Session.builder().id(sessionId).userId(1L).lastActivityAt(LocalDateTime.now()).build();
    when(database.getSessionById(sessionId)).thenReturn(Optional.of(validSession));

    Optional<Session> result = sessionService.validateSession(sessionId);

    assertTrue(result.isPresent());
    assertEquals(sessionId, result.get().getId());
    verify(database).updateSessionActivity(sessionId);
  }

  @Test
  void hasActiveSession_UserWithValidSession_ReturnsTrue() {
    Long userId = 1L;
    Long sessionId = 1L;
    Session validSession =
        Session.builder().id(sessionId).userId(userId).lastActivityAt(LocalDateTime.now()).build();

    when(database.getActiveSession(userId)).thenReturn(Optional.of(validSession));
    when(database.getSessionById(sessionId)).thenReturn(Optional.of(validSession));

    boolean result = sessionService.hasActiveSession(userId);

    assertTrue(result);
    verify(database).updateSessionActivity(sessionId);
  }

  @Test
  void hasActiveSession_UserWithExpiredSession_ReturnsFalse() {
    Long userId = 1L;
    Long sessionId = 1L;
    Session expiredSession =
        Session.builder()
            .id(sessionId)
            .userId(userId)
            .lastActivityAt(LocalDateTime.now().minusHours(1))
            .build();

    when(database.getActiveSession(userId)).thenReturn(Optional.of(expiredSession));
    when(database.getSessionById(sessionId)).thenReturn(Optional.of(expiredSession));

    boolean result = sessionService.hasActiveSession(userId);

    assertFalse(result);
    verify(database).deleteSession(sessionId);
  }

  @Test
  void hasActiveSession_UserWithNoSession_ReturnsFalse() {
    when(database.getActiveSession(anyLong())).thenReturn(Optional.empty());

    boolean result = sessionService.hasActiveSession(1L);

    assertFalse(result);
  }
}
