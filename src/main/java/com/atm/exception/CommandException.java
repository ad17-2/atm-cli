package com.atm.exception;

public class CommandException extends RuntimeException {
  public CommandException(String message) {
    super(message);
  }
}
