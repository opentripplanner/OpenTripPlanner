package org.opentripplanner.transit.model.filter.expr;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class OrMatcherTest {

  @Test
  void testMatch() {
    var matcher = OrMatcher.of(
      new EqualityMatcher<>("int", 42, i -> i),
      new EqualityMatcher<>("int", 43, i -> i)
    );
    assertTrue(matcher.match(42));
    assertTrue(matcher.match(43));
    assertFalse(matcher.match(44));
  }

  @Test
  void testMatchComposites() {
    var matcher = OrMatcher.of(
      AndMatcher.of(List.of(new EqualityMatcher<>("int", 42, i -> i))),
      AndMatcher.of(List.of(new EqualityMatcher<>("int", 43, i -> i)))
    );
    assertTrue(matcher.match(42));
    assertTrue(matcher.match(43));
    assertFalse(matcher.match(44));
  }
}
