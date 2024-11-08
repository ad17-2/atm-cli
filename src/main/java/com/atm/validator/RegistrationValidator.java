package com.atm.validator;

public class RegistrationValidator {

  public static void validateUsername(String username) {
    if (username == null || username.trim().isEmpty()) {
      throw new IllegalArgumentException("Username cannot be empty.");
    }
    if (username.length() < 3 || username.length() > 30) {
      throw new IllegalArgumentException("Username must be between 3 and 30 characters.");
    }
    if (!username.matches("^[a-zA-Z0-9]+$")) {
      throw new IllegalArgumentException("Username must contain only alphanumeric characters.");
    }
  }

  public static void validatePassword(String password) {
    if (password == null || password.trim().isEmpty()) {
      throw new IllegalArgumentException("Password cannot be empty.");
    }
    if (password.length() < 8) {
      throw new IllegalArgumentException("Password must be at least 8 characters long.");
    }
    if (!password.matches("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).+$")) {
      throw new IllegalArgumentException(
          "Password must contain at least one uppercase letter, one lowercase letter, and one number.");
    }
  }
}
