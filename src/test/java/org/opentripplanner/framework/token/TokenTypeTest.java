package org.opentripplanner.framework.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.Month;
import org.junit.jupiter.api.Test;

class TokenTypeTest {

  @Test
  void isNot() {
    assertTrue(TokenType.INT.isNot(TokenType.STRING));
    assertFalse(TokenType.INT.isNot(TokenType.INT));
  }

  @Test
  void valueToString() {
    assertEquals("", TokenType.BYTE.valueToString(null));
    assertEquals("34", TokenType.BYTE.valueToString((byte) 34));
    assertEquals("true", TokenType.BOOLEAN.valueToString(true));
    assertEquals("2m30s", TokenType.DURATION.valueToString(Duration.ofSeconds(150)));
    assertEquals("APRIL", TokenType.ENUM.valueToString(Month.APRIL));
    assertEquals("17", TokenType.INT.valueToString(17));
    assertEquals("FG", TokenType.STRING.valueToString("FG"));
    assertEquals(
      "2023-12-31T17:01:00Z",
      TokenType.TIME_INSTANT.valueToString(Instant.parse("2023-12-31T17:01:00Z"))
    );
  }

  @Test
  void stringToValue() {
    assertEquals((byte) 34, TokenType.BYTE.stringToValue("34"));
    assertEquals(true, TokenType.BOOLEAN.stringToValue("true"));
    assertEquals(Duration.ofSeconds(150), TokenType.DURATION.stringToValue("2m30s"));
    assertEquals("APRIL", TokenType.ENUM.stringToValue("APRIL"));
    assertEquals(17, TokenType.INT.stringToValue("17"));
    assertEquals("FG", TokenType.STRING.stringToValue("FG"));
    assertEquals(
      Instant.parse("2023-12-31T17:01:00Z"),
      TokenType.TIME_INSTANT.stringToValue("2023-12-31T17:01:00Z")
    );

    // Test nullable types with empty string
    assertNull(TokenType.DURATION.stringToValue(""));
    assertNull(TokenType.ENUM.stringToValue(""));
    assertNull(TokenType.STRING.stringToValue(""));
    assertNull(TokenType.TIME_INSTANT.stringToValue(""));
  }
}
