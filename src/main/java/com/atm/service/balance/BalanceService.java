package com.atm.service.balance;

import java.math.BigDecimal;

public interface BalanceService {

  BigDecimal getBalance(Long userId);
}
