package com.atm.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Balance {
  private Long userId;
  private BigDecimal balance;

  @Builder.Default private LocalDateTime lastUpdated = LocalDateTime.now();
}
