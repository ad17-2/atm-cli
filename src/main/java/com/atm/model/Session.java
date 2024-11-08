package com.atm.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Session {
  private Long id;
  private Long userId;
  private String username;

  @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
  private LocalDateTime lastActivityAt;

  private static final long SESSION_TIMEOUT_MINUTES = 1;

  private LocalDateTime expiresAt;

  public boolean isExpired() {
    if (lastActivityAt == null) {
      return true;
    }
    return LocalDateTime.now().isAfter(lastActivityAt.plusMinutes(SESSION_TIMEOUT_MINUTES));
  }
}
