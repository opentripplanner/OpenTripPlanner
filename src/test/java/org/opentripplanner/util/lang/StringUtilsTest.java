package org.opentripplanner.util.lang;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StringUtilsTest {

  @Test
  void hasValue() {
    assertTrue(StringUtils.hasValue("Text"));
    assertTrue(StringUtils.hasValue(" T "));
    assertFalse(StringUtils.hasValue(null));
    assertFalse(StringUtils.hasValue(""));
    assertFalse(StringUtils.hasValue(" "));
    assertFalse(StringUtils.hasValue("\n\t"));
  }

  @Test
  public void assertValueExist() {
    // Ok if any value
    StringUtils.assertHasValue("a");

    // Should fail for these values
    var illegalValues = new String[] { null, "", " ", "\t", " \n\r\t\f" };

    for (var it : illegalValues) {
      assertThrows(IllegalArgumentException.class, () -> StringUtils.assertHasValue(it));
    }
  }
}
