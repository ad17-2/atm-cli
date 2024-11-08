package com.atm.database;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DatabaseConfig {
  private final String jdbcUrl;
  private final String username;
  private final String password;
}
