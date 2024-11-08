package com.atm.exception;

public class ActiveSessionException extends RuntimeException {
  public ActiveSessionException(String message) {
    super(message);
  }
}
