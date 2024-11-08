package com.atm;

import com.atm.application.ATMFacade;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
  public static void main(String[] args) {
    try (ATMFacade app = new ATMFacade()) {
      app.start();
    } catch (Exception e) {
      log.error("Application error", e);
      System.exit(1);
    }
  }
}
