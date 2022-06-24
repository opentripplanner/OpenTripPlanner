package org.opentripplanner.util.lang;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class AssertUtilsTest {

  @Test
  public void assertValueExist() {
    // Ok if any value
    AssertUtils.assertHasValue("a");

    // Should fail for these values
    var illegalValues = new String[] { null, "", " ", "\t", " \n\r\t\f" };

    for (var it : illegalValues) {
      assertThrows(IllegalArgumentException.class, () -> AssertUtils.assertHasValue(it));
    }
  }
}
