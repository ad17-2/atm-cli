package com.atm.command;

import com.atm.exception.CommandException;
import com.atm.model.Session;
import com.atm.model.User;
import com.atm.service.balance.BalanceService;
import com.atm.service.session.SessionService;
import com.atm.service.transaction.TransactionService;
import com.atm.service.user.UserService;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TransferCommand implements Command {

  private final UserService userService;
  private final TransactionService transactionService;
  private final BalanceService balanceService;
  private final SessionHolder sessionHolder;
  private final SessionService sessionService;

  @Override
  public void execute(String... args) {
    if (args.length != 2) {
      throw new CommandException("Usage: transfer <username> <amount>");
    }

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

      Long userId = currentSession.getUserId();

      String targetUsername = args[0];

      BigDecimal transferAmount = null;
      try {
        transferAmount = new BigDecimal(args[1]);
      } catch (NumberFormatException e) {
        throw new CommandException("Invalid amount format");
      }

      log.info("TransferCommand: execute: user : {}", userId);

      Optional<User> targetUser = userService.getUserByUsername(targetUsername);

      if (!targetUser.isPresent()) {
        throw new CommandException("User not found");
      }

      BigDecimal balance = balanceService.getBalance(userId);

      if (balance.compareTo(transferAmount) < 0) {
        throw new CommandException("Insufficient balance");
      }

      transactionService.transfer(userId, targetUser.get().getId(), transferAmount);
      System.out.println("Transfer successful.");
    } catch (CommandException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new CommandException(e.getMessage());
    } catch (Exception e) {
      log.error("Failed to transfer money", e);
      throw new CommandException("Failed to transfer money");
    }
  }
}
