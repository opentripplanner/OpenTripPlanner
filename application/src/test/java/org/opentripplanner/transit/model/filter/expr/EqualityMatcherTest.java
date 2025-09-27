package org.opentripplanner.transit.model.filter.expr;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EqualityMatcherTest {

  @Test
  void testMatchesPrimitive() {
    var matcher = new EqualityMatcher<>("int", 42, i -> i);
    assertTrue(matcher.match(42));
    assertFalse(matcher.match(43));
  }

  @Test
  void testMatchesObject() {
    var matcher = new EqualityMatcher<>("string", "foo", s -> s);
    assertTrue(matcher.match("foo"));
    assertFalse(matcher.match("bar"));
  }
}
