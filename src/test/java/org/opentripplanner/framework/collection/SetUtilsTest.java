package org.opentripplanner.framework.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SetUtilsTest {

  @Test
  void combine() {
    var combined = SetUtils.combine(List.of(1, 2, 3), Set.of(2, 3, 4));
    assertEquals(Set.of(1, 2, 3, 4), combined);
  }
}
