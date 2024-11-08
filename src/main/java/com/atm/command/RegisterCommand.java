package com.atm.command;

import com.atm.exception.CommandException;
import com.atm.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RegisterCommand implements Command {
  private final UserService userService;

  @Override
  public void execute(String... args) {
    if (args.length != 2) {
      throw new CommandException("Usage: register <username> <password>");
    }

    String username = args[0];
    String password = args[1];

    try {
      userService.register(username, password);
      System.out.println("Registration successful with username: " + username);
    } catch (CommandException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new CommandException(e.getMessage());
    } catch (Exception e) {
      log.error("Registration failed", e);
      throw new CommandException("Registration failed: " + e.getMessage());
    }
  }
}
