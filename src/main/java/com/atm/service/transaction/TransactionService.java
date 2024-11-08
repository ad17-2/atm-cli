package com.atm.service.transaction;

import java.math.BigDecimal;

public interface TransactionService {
  void deposit(Long userId, BigDecimal amount);

  void withdraw(Long userId, BigDecimal amount);

  void transfer(Long fromUserId, Long toUserId, BigDecimal amount);
}
