package com.atm.service.session;

import com.atm.model.Session;
import java.util.Optional;

public interface SessionService {

  Long createSession(Long userId);

  void terminateSession(Long sessionId);

  Optional<Session> validateSession(Long sessionId);

  boolean hasActiveSession(Long userId);
}
