package com.atm.cli;

import com.atm.command.BalanceCommand;
import com.atm.command.Command;
import com.atm.command.DepositCommand;
import com.atm.command.LoginCommand;
import com.atm.command.LogoutCommand;
import com.atm.command.RegisterCommand;
import com.atm.command.SessionHolder;
import com.atm.command.TransferCommand;
import com.atm.command.WithdrawCommand;
import com.atm.exception.CommandException;
import com.atm.service.balance.BalanceService;
import com.atm.service.session.SessionService;
import com.atm.service.transaction.TransactionService;
import com.atm.service.user.UserService;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ATMCli {
  private final Map<String, Command> commands;
  private final CLIHandler cliHandler;
  private final SessionHolder sessionHolder;
  private volatile boolean running;
  private final Object lock = new Object();

  public ATMCli(
      UserService userService,
      SessionService sessionService,
      BalanceService balanceService,
      TransactionService transactionService,
      CLIHandler cliHandler) {
    this.cliHandler = cliHandler;
    this.sessionHolder = new SessionHolder();
    this.commands = new HashMap<>();
    initializeCommands(userService, sessionService, balanceService, transactionService);
  }

  private void initializeCommands(
      UserService userService,
      SessionService sessionService,
      BalanceService balanceService,
      TransactionService transactionService) {
    commands.put("register", new RegisterCommand(userService));
    commands.put("login", new LoginCommand(userService, sessionHolder));
    commands.put("logout", new LogoutCommand(sessionService, sessionHolder));
    commands.put(
        "deposit",
        new DepositCommand(transactionService, balanceService, sessionHolder, sessionService));
    commands.put(
        "withdraw",
        new WithdrawCommand(transactionService, balanceService, sessionHolder, sessionService));
    commands.put(
        "transfer",
        new TransferCommand(userService, transactionService, sessionHolder, sessionService));
    commands.put("balance", new BalanceCommand(balanceService, sessionHolder, sessionService));
  }

  public void start() {
    synchronized (lock) {
      if (running) {
        throw new IllegalStateException("CLI is already running");
      }
      running = true;
    }

    log.info("Starting ATM CLI...");
    cliHandler.print("Welcome to ATM CLI!");
    cliHandler.print("Type 'help' for available commands, 'exit' to quit.");

    while (isRunning()) {
      try {
        cliHandler.print("> ");
        String input = cliHandler.readLine().trim();

        if (input.isEmpty()) {
          continue;
        }

        if (input.equalsIgnoreCase("exit")) {
          stop();
          break;
        }

        if (input.equalsIgnoreCase("help")) {
          showHelp();
          continue;
        }

        processCommand(input);
      } catch (Exception e) {
        log.error("Error processing command", e);
        cliHandler.printError(e.getMessage());
      }
    }
    log.info("ATM CLI stopped");
  }

  private void processCommand(String input) {
    String[] parts = input.split("\\s+");
    String commandName = parts[0].toLowerCase();

    Command command = commands.get(commandName);
    if (command == null) {
      cliHandler.printError("Unknown command. Type 'help' for available commands.");
      return;
    }

    String[] args = new String[parts.length - 1];
    System.arraycopy(parts, 1, args, 0, args.length);

    try {
      command.execute(args);
    } catch (CommandException e) {
      cliHandler.printError(e.getMessage());
    }
  }

  private void showHelp() {
    cliHandler.print("Available commands:");
    cliHandler.print("  register <username> <password> - Create a new account");
    cliHandler.print("  login <username> <password>    - Log into your account");
    cliHandler.print("  logout                         - Log out of current account");
    cliHandler.print("  deposit <amount>               - Deposit money");
    cliHandler.print("  withdraw <amount>              - Withdraw money");
    cliHandler.print("  transfer <username> <amount>   - Transfer money to another user");
    cliHandler.print("  balance                        - Check your balance");
    cliHandler.print("  help                           - Show this help message");
    cliHandler.print("  exit                           - Exit the application");
  }

  public void stop() {
    synchronized (lock) {
      if (!running) {
        return;
      }
      running = false;

      if (sessionHolder.getCurrentSession() != null) {
        try {
          commands.get("logout").execute();
        } catch (Exception e) {
          log.error("Error during logout on shutdown", e);
        }
      }

      cliHandler.print("Shutting down...");
    }
  }

  private boolean isRunning() {
    synchronized (lock) {
      return running;
    }
  }
}
