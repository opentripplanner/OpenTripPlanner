package org.opentripplanner.framework.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ListSectionTest {

  @Test
  void isHead() {
    assertTrue(ListSection.HEAD.isHead());
    assertFalse(ListSection.TAIL.isHead());
  }

  @Test
  void oppositeEnd() {
    assertEquals(ListSection.HEAD, ListSection.TAIL.invert());
    assertEquals(ListSection.TAIL, ListSection.HEAD.invert());
  }
}
