package org.opentripplanner.utils.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.utils.lang.ArrayUtils.hasContent;

import org.junit.jupiter.api.Test;

class ArrayUtilsTest {

  @Test
  void testHasContent() {
    assertFalse(hasContent(null));
    assertFalse(hasContent(new String[] {}));
    assertTrue(hasContent(new Double[] { 0.0 }));
  }

  @Test
  void reversedCopyMultipleElements(){
    int[] original = new int[] { 1, 3, 4 };
    int[] reversed = ArrayUtils.reversedCopy(original);
    assertEquals(4, reversed[0]);
    assertEquals(3, reversed[1]);
    assertEquals(1, reversed[2]);
  }

  @Test
  void reversedCopySingleElement(){
    int[] original = new int[] { 1 };
    int[] reversed = ArrayUtils.reversedCopy(original);
    assertEquals(1, reversed[0]);
  }
}
