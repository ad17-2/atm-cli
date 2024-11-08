package com.atm.service.user;

import com.atm.database.Database;
import com.atm.exception.ActiveSessionException;
import com.atm.model.Session;
import com.atm.model.User;
import com.atm.service.session.SessionService;
import com.atm.validator.RegistrationValidator;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;

@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private final Database database;
  private final SessionService sessionService;

  @Override
  public User register(String username, String password) {
    log.info("Attempting to register user: {}", username);

    RegistrationValidator.validateUsername(username);
    RegistrationValidator.validatePassword(password);

    String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());

    User user = database.getUserByUsername(username).orElse(null);

    if (user != null) {
      log.warn("User already exists: {}", username);
      throw new IllegalArgumentException("User already exists");
    }

    Long userId = database.createUser(username, passwordHash);

    log.info("Successfully registered user: {}", username);

    return User.builder().id(userId).username(username).passwordHash(passwordHash).build();
  }

  @Override
  public Optional<Session> login(String username, String password) {
    log.info("Attempting login for user: {}", username);

    return this.getUserByUsername(username)
        .map(
            user -> {
              if (sessionService.hasActiveSession(user.getId())) {
                log.warn("User {} already has an active session", username);
                throw new ActiveSessionException("User already logged in. Please logout first.");
              }

              if (!BCrypt.checkpw(password, user.getPasswordHash())) {
                log.warn("Invalid password attempt for user: {}", username);
                return null;
              }

              Long sessionId = sessionService.createSession(user.getId());
              database.updateLastLogin(user.getId());
              log.info("User logged in successfully: {}", username);

              return Session.builder()
                  .id(sessionId)
                  .userId(user.getId())
                  .username(user.getUsername())
                  .build();
            })
        .filter(session -> session != null);
  }

  @Override
  public void logout(Session session) {
    if (session != null) {
      log.info("Logging out user ID: {}", session.getUserId());
      sessionService.terminateSession(session.getId());
    }
  }

  @Override
  public Optional<User> getUserByUsername(String username) {
    return database.getUserByUsername(username);
  }
}
