package com.atm.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

@Data
@Builder
public class User {
  private Long id;
  private String username;
  private String passwordHash;

  @Default private LocalDateTime createdAt = LocalDateTime.now();

  private LocalDateTime lastLogin;
}
