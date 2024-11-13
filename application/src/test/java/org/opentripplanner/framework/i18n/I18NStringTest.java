package org.opentripplanner.framework.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class I18NStringTest {

  private final I18NString noValue = I18NString.of(" \t\n\r\f");
  private final I18NString hasValue = I18NString.of("A value");

  @Test
  void hasValue() {
    assertTrue(I18NString.hasValue(hasValue));
    assertFalse(I18NString.hasValue(noValue));
  }

  @Test
  void hasNoValue() {
    assertFalse(I18NString.hasNoValue(hasValue));
    assertTrue(I18NString.hasNoValue(noValue));
  }

  @Test
  void assertHasValue() {
    var ex = assertThrows(IllegalArgumentException.class, () -> I18NString.assertHasValue(noValue));
    assertEquals("Value can not be null, empty or just whitespace: ' \t\n\r\f'", ex.getMessage());
  }
}
