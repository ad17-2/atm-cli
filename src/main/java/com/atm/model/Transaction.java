package com.atm.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Transaction {
  private Long id;
  private Long fromUserId;
  private Long toUserId;
  private BigDecimal amount;
  private TransactionType type;

  @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();

  public enum TransactionType {
    DEPOSIT,
    WITHDRAW,
    TRANSFER
  }
}
