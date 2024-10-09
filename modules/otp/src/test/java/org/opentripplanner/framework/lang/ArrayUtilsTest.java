package org.opentripplanner.framework.lang;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.framework.lang.ArrayUtils.hasContent;

import org.junit.jupiter.api.Test;

class ArrayUtilsTest {

  @Test
  void testHasContent() {
    assertFalse(hasContent(null));
    assertFalse(hasContent(new String[] {}));
    assertTrue(hasContent(new Double[] { 0.0 }));
  }
}
