package com.atm.service.session;

import com.atm.database.Database;
import com.atm.model.Session;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {
  private final Database database;

  @Override
  public Long createSession(Long userId) {
    log.info("Creating session for user: {}", userId);
    return database.createSession(userId);
  }

  @Override
  public void terminateSession(Long sessionId) {
    log.info("Terminating session: {}", sessionId);
    database.deleteSession(sessionId);
  }

  @Override
  public Optional<Session> validateSession(Long sessionId) {
    if (sessionId == null) {
      return Optional.empty();
    }

    return database
        .getSessionById(sessionId)
        .filter(
            session -> {
              if (session.isExpired()) {
                log.debug("Session expired: {}", sessionId);
                terminateSession(sessionId);
                return false;
              }
              database.updateSessionActivity(sessionId);
              return true;
            });
  }

  @Override
  public boolean hasActiveSession(Long userId) {
    return database
        .getActiveSession(userId)
        .map(session -> validateSession(session.getId()).isPresent())
        .orElse(false);
  }
}
