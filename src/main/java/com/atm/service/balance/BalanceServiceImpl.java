package com.atm.service.balance;

import com.atm.database.Database;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class BalanceServiceImpl implements BalanceService {

  private final Database database;

  @Override
  public BigDecimal getBalance(Long userId) {
    return database.getBalance(userId);
  }
}
