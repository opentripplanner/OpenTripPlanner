package org.opentripplanner.utils.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SetUtilsTest {

  @Test
  void combine() {
    var combined = SetUtils.combine(List.of(1, 2, 3), Set.of(2, 3, 4));
    assertEquals(Set.of(1, 2, 3, 4), combined);
  }

  @Test
  void intersection() {
    var input = new ArrayList<Set<Integer>>();
    input.add(Set.of(1, 2, 3));
    input.add(Set.of(2, 3));
    input.add(Set.of(2, 3));
    input.add(Set.of(4, 3, 2));

    var res = SetUtils.intersection(input);
    assertEquals(Set.of(2, 3), res);
  }
}
