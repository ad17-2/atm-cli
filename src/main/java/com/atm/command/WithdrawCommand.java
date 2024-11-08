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
public class WithdrawCommand implements Command {

  private final TransactionService transactionService;
  private final BalanceService balanceService;
  private final SessionHolder sessionHolder;
  private final SessionService sessionService;

  @Override
  public void execute(String... args) {

    if (args.length != 1) {
      throw new CommandException("Usage: withdraw <amount>");
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
      BigDecimal withdrawAmount = new BigDecimal(args[0]);

      if (withdrawAmount.compareTo(BigDecimal.ONE) <= 0) {
        throw new CommandException("Invalid amount, must be grater than 1");
      }

      log.info("WithdrawCommand: execute: user : {}", userId);

      BigDecimal balance = balanceService.getBalance(userId);

      if (balance.compareTo(withdrawAmount) < 0) {
        throw new CommandException("Insufficient balance");
      }

      transactionService.withdraw(userId, withdrawAmount);

      BigDecimal newBalance = balanceService.getBalance(userId);

      System.out.println("Withdraw successful. New balance: $" + newBalance);
    } catch (CommandException e) {
      throw e;
    } catch (NumberFormatException e) {
      throw new CommandException("Invalid amount format");
    } catch (Exception e) {
      throw new CommandException("Failed to withdraw money");
    }
  }
}
