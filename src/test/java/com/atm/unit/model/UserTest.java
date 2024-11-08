package com.atm.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.atm.model.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void whenUserCreatedWithValidData_thenSucceeds() {
    User user = User.builder().id(1L).username("testuser").passwordHash("hashedpassword").build();

    assertEquals(1L, user.getId());
    assertEquals("testuser", user.getUsername());
    assertEquals("hashedpassword", user.getPasswordHash());
    assertNull(user.getLastLogin());
  }

  @Test
  void whenUserBuiltWithoutOptionalFields_thenSucceeds() {
    User user = User.builder().username("testuser").passwordHash("hashedpassword").build();

    assertNull(user.getId());
    assertNotNull(user.getCreatedAt());
    assertNull(user.getLastLogin());
  }

  @Test
  void whenLastLoginUpdated_thenSucceeds() {
    User user = User.builder().username("testuser").passwordHash("hashedpassword").build();

    LocalDateTime loginTime = LocalDateTime.now();

    user.setLastLogin(loginTime);

    assertEquals(loginTime, user.getLastLogin());
  }
}
