package com.atm.command;

import com.atm.exception.CommandException;
import com.atm.model.Session;
import com.atm.service.balance.BalanceService;
import com.atm.service.session.SessionService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class BalanceCommand implements Command {

  private final BalanceService balanceService;
  private final SessionHolder sessionHolder;
  private final SessionService sessionService;

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

      BigDecimal balance = balanceService.getBalance(currentSession.getUserId());

      System.out.println("Balance: $" + balance);
    } catch (CommandException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to get balance", e);
      throw new CommandException("Failed to get balance user");
    }
  }
}
