package org.opentripplanner.util.lang;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
