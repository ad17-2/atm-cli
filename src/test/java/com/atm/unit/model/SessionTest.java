package com.atm.unit.model;

import static org.junit.jupiter.api.Assertions.*;

import com.atm.model.Session;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SessionTest {

  @Test
  void whenSessionCreatedWithValidData_thenSucceeds() {
    LocalDateTime now = LocalDateTime.now();

    Session session = Session.builder().id(1L).userId(1L).createdAt(now).build();

    assertEquals(1L, session.getId());
    assertEquals(1L, session.getUserId());
    assertEquals(now, session.getCreatedAt());
  }

  @Test
  void whenSessionBuiltWithoutOptionalFields_thenSucceeds() {
    Session session = Session.builder().userId(1L).build();

    assertNull(session.getId());
    assertNotNull(session.getCreatedAt());
  }
}
