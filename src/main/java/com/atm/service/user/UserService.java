package com.atm.service.user;

import com.atm.model.Session;
import com.atm.model.User;
import java.util.Optional;

public interface UserService {
  User register(String username, String password);

  Optional<Session> login(String username, String password);

  void logout(Session session);

  Optional<User> getUserByUsername(String username);
}
