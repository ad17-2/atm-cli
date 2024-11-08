package com.atm.cli;

public interface CLIHandler {
  String readLine();

  void print(String message);

  void printError(String message);

  void printSuccess(String message);
}
