package com.atm.command;

import com.atm.exception.ActiveSessionException;
import com.atm.exception.CommandException;
import com.atm.model.Session;
import com.atm.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class LoginCommand implements Command {

  private final UserService userService;
  private final SessionHolder sessionHolder;

  @Override
  public void execute(String... args) {

    if (args.length != 2) {
      throw new CommandException("Usage: login <username> <password>");
    }

    String username = args[0];
    String password = args[1];

    try {
      if (sessionHolder.getCurrentSession() != null) {
        throw new ActiveSessionException("User already logged in. Please logout first.");
      }

      Session session =
          userService
              .login(username, password)
              .orElseThrow(() -> new CommandException("Invalid Credentials"));

      sessionHolder.setCurrentSession(session);
      log.info("User {} logged in successfully", username);
      System.out.println("Hello, " + username);
    } catch (ActiveSessionException e) {
      throw new CommandException(e.getMessage());
    } catch (CommandException e) {
      throw new CommandException(e.getMessage());
    } catch (Exception e) {
      log.error("Failed to login user: {}", username, e);
      throw new CommandException("Failed to login user");
    }
  }
}
