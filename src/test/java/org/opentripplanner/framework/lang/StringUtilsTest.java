package org.opentripplanner.framework.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

      Throwable thrown = assertThrows(
        IllegalArgumentException.class,
        () -> StringUtils.assertHasValue(it, "Illegal value %s", it)
      );
      assertTrue(thrown.getMessage().startsWith("Illegal value " + it));
    }
  }

  @Test
  void padLeft() {
    assertEquals("?????ABC", StringUtils.padLeft("ABC", '?', 8));
    assertEquals("ABC", StringUtils.padLeft("ABC", '?', 3));
    assertEquals("????????", StringUtils.padLeft(null, '?', 8));
  }

  @Test
  void padCenter() {
    assertEquals("??AB??", StringUtils.padBoth("AB", '?', 6));
    assertEquals("???AB??", StringUtils.padBoth("AB", '?', 7));
    assertEquals("??ABC?", StringUtils.padBoth("ABC", '?', 6));
    assertEquals("??ABC??", StringUtils.padBoth("ABC", '?', 7));
    assertEquals("ABC", StringUtils.padBoth("ABC", '?', 3));
    assertEquals("????????", StringUtils.padBoth(null, '?', 8));
  }

  @Test
  void padRight() {
    assertEquals("ABC???", StringUtils.padRight("ABC", '?', 6));
    assertEquals("??????", StringUtils.padRight(null, '?', 6));
    assertEquals("ABC", StringUtils.padRight("ABC", '?', 3));
  }

  @Test
  void quoteReplace() {
    assertEquals("\"key\" : \"value\"", StringUtils.quoteReplace("'key' : 'value'"));
  }
}
