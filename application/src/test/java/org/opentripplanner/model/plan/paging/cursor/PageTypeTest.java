package org.opentripplanner.model.plan.paging.cursor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PageTypeTest {

  @Test
  void isNext() {
    assertTrue(PageType.NEXT_PAGE.isNext());
    assertFalse(PageType.PREVIOUS_PAGE.isNext());
  }
}
