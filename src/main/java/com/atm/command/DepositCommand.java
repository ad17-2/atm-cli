package com.atm.command;

import com.atm.exception.CommandException;
import com.atm.model.Session;
import com.atm.service.balance.BalanceService;
import com.atm.service.session.SessionService;
import com.atm.service.transaction.TransactionService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DepositCommand implements Command {

  private final TransactionService transactionService;
  private final BalanceService balanceService;
  private final SessionHolder sessionHolder;
  private final SessionService sessionService;

  @Override
  public void execute(String... args) {

    if (args.length != 1) {
      throw new CommandException("Usage: deposit <amount>");
    }

    try {

      Session currentSession = sessionHolder.getCurrentSession();

      if (currentSession == null) {
        throw new CommandException("No active session, Please login first!");
      }

      boolean hasActiveSession = sessionService.hasActiveSession(currentSession.getUserId());

      Long userId = currentSession.getUserId();

      if (!hasActiveSession) {
        sessionHolder.terminateSession();
        throw new CommandException("No active session, Please login first!");
      }

      BigDecimal depositAmount = null;

      try {
        depositAmount = new BigDecimal(args[1]);
      } catch (NumberFormatException e) {
        throw new CommandException("Invalid amount format");
      }

      log.info("DepositCommand: execute: user : {}", userId);

      transactionService.deposit(userId, depositAmount);
      BigDecimal balance = balanceService.getBalance(userId);

      System.out.println("Deposit successful. New balance: $" + balance);
    } catch (CommandException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new CommandException(e.getMessage());
    } catch (Exception e) {
      throw new CommandException("Failed to deposit money");
    }
  }
}
