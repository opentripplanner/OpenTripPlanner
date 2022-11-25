package org.opentripplanner.framework.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ListUtilsTest {

  @Test
  void combine() {
    var combined = ListUtils.combine(List.of(1, 2, 3), List.of(5, 6, 7));
    assertEquals(List.of(1, 2, 3, 5, 6, 7), combined);
  }
}
