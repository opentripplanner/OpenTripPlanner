package org.opentripplanner.transit.model.filter.expr;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContainsMatcherTest {

  private static final Map<Integer, List<String>> integerListMap = Map.of(
    1,
    List.of("foo"),
    2,
    List.of("bar"),
    3,
    List.of("foo", "bar")
  );

  @Test
  void testMatch() {
    var matcher = new ContainsMatcher<>(
      "contains",
      integerListMap::get,
      new EqualityMatcher<>("string", "foo", s -> s)
    );

    assertTrue(matcher.match(1));
    assertFalse(matcher.match(2));
    assertTrue(matcher.match(3));
    assertFalse(matcher.match(4));
  }
}
