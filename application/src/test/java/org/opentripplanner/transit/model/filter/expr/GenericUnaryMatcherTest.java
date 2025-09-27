package org.opentripplanner.transit.model.filter.expr;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GenericUnaryMatcherTest {

  @Test
  void testMatches() {
    var matcher = new GenericUnaryMatcher<>("int", i -> i.equals(42));
    assertTrue(matcher.match(42));
    assertFalse(matcher.match(43));
  }
}
