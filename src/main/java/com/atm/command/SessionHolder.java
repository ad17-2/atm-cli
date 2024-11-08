package com.atm.command;

import com.atm.model.Session;
import lombok.Data;

@Data
public class SessionHolder {

  private Session currentSession;

  public void terminateSession() {
    this.currentSession = null;
  }
}
