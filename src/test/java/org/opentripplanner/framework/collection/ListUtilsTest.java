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

  private static String makeHello() {
    return new String("HELLO");
  }

  @Test
  void distinctByKey() {
    var first = new Wrapper(10, makeHello());
    var last = new Wrapper(20, "HI");
    var duplicates = List.of(first, new Wrapper(20, makeHello()), last);

    var deduplicated = ListUtils.distinctByKey(duplicates, w -> w.string);

    assertEquals(List.of(first, last), deduplicated);
  }

  private record Wrapper(int i, String string) {}
}
