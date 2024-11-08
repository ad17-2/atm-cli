package com.atm.cli;

import java.util.Scanner;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CLIHandlerImpl implements CLIHandler, AutoCloseable {
  private final Scanner scanner;
  private volatile boolean closed = false;

  public CLIHandlerImpl() {
    this.scanner = new Scanner(System.in);
  }

  @Override
  public String readLine() {
    if (closed) {
      throw new IllegalStateException("IOHandler is closed");
    }
    try {
      return scanner.nextLine();
    } catch (IllegalStateException e) {
      return "exit";
    }
  }

  @Override
  public void print(String message) {
    System.out.println(message);
  }

  @Override
  public void printError(String message) {
    System.err.println("Error: " + message);
  }

  @Override
  public void printSuccess(String message) {
    System.out.println("Success: " + message);
  }

  @Override
  public void close() {
    closed = true;
    scanner.close();
  }
}
