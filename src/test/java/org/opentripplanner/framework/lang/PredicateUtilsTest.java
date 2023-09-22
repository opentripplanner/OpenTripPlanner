package org.opentripplanner.framework.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PredicateUtilsTest {

  private static String makeHello() {
    return new String("HELLO");
  }

  @Test
  void distinctByKey() {
    var first = new Wrapper(10, makeHello());
    var last = new Wrapper(20, "HI");
    var stream = Stream.of(first, new Wrapper(20, makeHello()), last);

    var deduplicated = stream.filter(PredicateUtils.distinctByKey(w -> w.string)).toList();

    assertEquals(List.of(first, last), deduplicated);
  }

  private record Wrapper(int i, String string) {}
}
