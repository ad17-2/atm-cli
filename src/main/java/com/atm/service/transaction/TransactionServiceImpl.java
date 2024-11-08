package com.atm.service.transaction;

import com.atm.database.Database;
import com.atm.exception.InsufficientFundsException;
import com.atm.service.balance.BalanceService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
  private final Database database;
  private final BalanceService balanceService;

  @Override
  public void deposit(Long userId, BigDecimal amount) {
    try {
      log.info("Attempting to deposit {} for user {}", amount, userId);

      if (amount.compareTo(BigDecimal.ONE) <= 0) {
        throw new IllegalArgumentException("Invalid amount, must be grater than 1");
      }

      BigDecimal currentBalance = balanceService.getBalance(userId);
      BigDecimal newBalance = currentBalance.add(amount);

      database.createTransaction(userId, amount, "DEPOSIT");

      log.info("Deposit successful. New balance: {}", newBalance);
    } catch (Exception e) {
      log.error("Failed to deposit money", e);
    }
  }

  @Override
  public void withdraw(Long userId, BigDecimal amount) {
    log.info("Attempting to withdraw {} for user {}", amount, userId);

    BigDecimal currentBalance = balanceService.getBalance(userId);

    if (currentBalance.compareTo(amount) < 0) {
      log.warn(
          "Insufficient funds for withdrawal. Current balance: {}, Requested: {}",
          currentBalance,
          amount);
      throw new InsufficientFundsException("Insufficient funds for withdrawal");
    }

    if (amount.compareTo(BigDecimal.ONE) <= 0) {
      throw new IllegalArgumentException("Invalid amount, must be grater than 1");
    }

    BigDecimal newBalance = currentBalance.subtract(amount);

    database.createTransaction(userId, amount, "WITHDRAW");

    log.info("Withdrawal successful. New balance: {}", newBalance);
  }

  @Override
  public void transfer(Long fromUserId, Long toUserId, BigDecimal amount) {
    log.info("Attempting to transfer {} from user {} to user {}", amount, fromUserId, toUserId);

    BigDecimal fromBalance = balanceService.getBalance(fromUserId);
    if (fromBalance.compareTo(amount) < 0) {
      log.warn(
          "Insufficient funds for transfer. Current balance: {}, Requested: {}",
          fromBalance,
          amount);
      throw new InsufficientFundsException("Insufficient funds for transfer");
    }

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Transfer amount must be positive");
    }

    if (amount.compareTo(BigDecimal.ONE) <= 0) {
      throw new IllegalArgumentException("Invalid amount, must be grater than 1");
    }

    if (fromUserId.equals(toUserId)) {
      throw new IllegalArgumentException("Cannot transfer to same account");
    }

    database.performTransfer(fromUserId, toUserId, amount);

    log.info("Transfer successful");
  }
}
