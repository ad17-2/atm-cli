package com.atm.command;

import com.atm.exception.CommandException;
import com.atm.model.Session;
import com.atm.service.session.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class LogoutCommand implements Command {

  private final SessionService sessionService;
  private final SessionHolder sessionHolder;

  @Override
  public void execute(String... args) {

    try {
      Session currentSession = sessionHolder.getCurrentSession();

      if (currentSession == null) {
        throw new CommandException("No active session, Please login first!");
      }

      boolean hasActiveSession = sessionService.hasActiveSession(currentSession.getUserId());

      if (!hasActiveSession) {
        sessionHolder.terminateSession();
        throw new CommandException("No active session, Please login first!");
      }

      System.out.println("Goodbye, " + currentSession.getUsername());
      sessionHolder.terminateSession();
      sessionService.terminateSession(currentSession.getId());
    } catch (CommandException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to logout", e);
      throw new CommandException("Failed to logout");
    }
  }
}
