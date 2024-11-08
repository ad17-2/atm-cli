package com.atm.application;

import com.atm.cli.ATMCli;
import com.atm.cli.CLIHandler;
import com.atm.cli.CLIHandlerImpl;
import com.atm.database.Database;
import com.atm.service.balance.BalanceService;
import com.atm.service.balance.BalanceServiceImpl;
import com.atm.service.session.SessionService;
import com.atm.service.session.SessionServiceImpl;
import com.atm.service.transaction.TransactionService;
import com.atm.service.transaction.TransactionServiceImpl;
import com.atm.service.user.UserService;
import com.atm.service.user.UserServiceImpl;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ATMFacade implements AutoCloseable {
  private final Database database;
  private final SessionService sessionService;
  private final UserService userService;
  private final TransactionService transactionService;
  private final BalanceService balanceService;
  private final ATMCli cli;

  public ATMFacade() {
    this(new CLIHandlerImpl());
  }

  public ATMFacade(CLIHandler cliHandler) {
    this.database = new Database();
    this.sessionService = new SessionServiceImpl(database);
    this.balanceService = new BalanceServiceImpl(database);
    this.userService = new UserServiceImpl(database, sessionService);
    this.transactionService = new TransactionServiceImpl(database, balanceService);
    this.cli =
        new ATMCli(userService, sessionService, balanceService, transactionService, cliHandler);
  }

  public void start() {
    log.info("Starting ATM application...");
    try {
      registerShutdownHook();
      cli.start();
    } catch (Exception e) {
      log.error("Failed to start ATM application", e);
      shutdown();
      throw new RuntimeException("Failed to start ATM application", e);
    }
  }

  /*
   * Register a shutdown hook to gracefully shutdown the application when the JVM is shutting down.
   * https://www.baeldung.com/jvm-shutdown-hooks
   */
  private void registerShutdownHook() {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  log.info("Shutdown hook triggered, initiating graceful shutdown...");
                  shutdown();
                },
                "shutdown-hook"));
  }

  private void shutdown() {
    try {
      log.info("Shutting down ATM application...");
      cli.stop();
      database.close();
      log.info("ATM application shutdown complete");
    } catch (Exception e) {
      log.error("Error during shutdown", e);
    }
  }

  @Override
  public void close() {
    shutdown();
  }
}
