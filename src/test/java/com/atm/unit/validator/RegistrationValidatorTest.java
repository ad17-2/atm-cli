package com.atm.unit.validator;

import static org.junit.jupiter.api.Assertions.*;

import com.atm.validator.RegistrationValidator;
import org.junit.jupiter.api.Test;

class RegistrationValidatorTest {

  @Test
  void testValidUsername() {
    assertDoesNotThrow(() -> RegistrationValidator.validateUsername("ValidUser123"));
  }

  @Test
  void testUsernameIsNull() {
    Exception exception =
        assertThrows(
            IllegalArgumentException.class, () -> RegistrationValidator.validateUsername(null));
    assertEquals("Username cannot be empty.", exception.getMessage());
  }

  @Test
  void testUsernameIsEmpty() {
    Exception exception =
        assertThrows(
            IllegalArgumentException.class, () -> RegistrationValidator.validateUsername("   "));
    assertEquals("Username cannot be empty.", exception.getMessage());
  }

  @Test
  void testUsernameTooShort() {
    Exception exception =
        assertThrows(
            IllegalArgumentException.class, () -> RegistrationValidator.validateUsername("AB"));
    assertEquals("Username must be between 3 and 30 characters.", exception.getMessage());
  }

  @Test
  void testUsernameTooLong() {

    final StringBuilder longUsernameBuilder = new StringBuilder();

    for (int i = 0; i < 31; i++) {
      longUsernameBuilder.append("a");
    }

    final String longUsername = longUsernameBuilder.toString();

    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> RegistrationValidator.validateUsername(longUsername));
    assertEquals("Username must be between 3 and 30 characters.", exception.getMessage());
  }

  @Test
  void testUsernameNonAlphanumeric() {
    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> RegistrationValidator.validateUsername("Invalid@User"));
    assertEquals("Username must contain only alphanumeric characters.", exception.getMessage());
  }

  @Test
  void testValidPassword() {
    assertDoesNotThrow(() -> RegistrationValidator.validatePassword("ValidPass1"));
  }

  @Test
  void testPasswordIsNull() {
    Exception exception =
        assertThrows(
            IllegalArgumentException.class, () -> RegistrationValidator.validatePassword(null));
    assertEquals("Password cannot be empty.", exception.getMessage());
  }

  @Test
  void testPasswordIsEmpty() {
    Exception exception =
        assertThrows(
            IllegalArgumentException.class, () -> RegistrationValidator.validatePassword("   "));
    assertEquals("Password cannot be empty.", exception.getMessage());
  }

  @Test
  void testPasswordTooShort() {
    Exception exception =
        assertThrows(
            IllegalArgumentException.class, () -> RegistrationValidator.validatePassword("Pass1"));
    assertEquals("Password must be at least 8 characters long.", exception.getMessage());
  }

  @Test
  void testPasswordNoUppercase() {
    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> RegistrationValidator.validatePassword("password1"));
    assertEquals(
        "Password must contain at least one uppercase letter, one lowercase letter, and one number.",
        exception.getMessage());
  }

  @Test
  void testPasswordNoLowercase() {
    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> RegistrationValidator.validatePassword("PASSWORD1"));
    assertEquals(
        "Password must contain at least one uppercase letter, one lowercase letter, and one number.",
        exception.getMessage());
  }

  @Test
  void testPasswordNoNumber() {
    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> RegistrationValidator.validatePassword("Password"));
    assertEquals(
        "Password must contain at least one uppercase letter, one lowercase letter, and one number.",
        exception.getMessage());
  }
}
