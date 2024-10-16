package org.opentripplanner.transit.model.filter.expr;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class AndMatcherTest {

  @Test
  void testMatchSingleMatcher() {
    var matcher = AndMatcher.of(List.of(new EqualityMatcher<>("int", 42, i -> i)));
    assertTrue(matcher.match(42));
    assertFalse(matcher.match(43));
  }

  @Test
  void testMatchMultiple() {
    var matcher = AndMatcher.of(
      List.of(new EqualityMatcher<>("int", 42, i -> i), new EqualityMatcher<>("int", 43, i -> i))
    );
    assertFalse(matcher.match(42));
    assertFalse(matcher.match(43));
    assertFalse(matcher.match(44));
  }

  @Test
  void testMatchComposites() {
    var matcher = AndMatcher.of(
      List.of(
        OrMatcher.of(List.of(new EqualityMatcher<>("int", 42, i -> i))),
        OrMatcher.of(List.of(new EqualityMatcher<>("int", 43, i -> i)))
      )
    );
    assertFalse(matcher.match(42));
    assertFalse(matcher.match(43));
    assertFalse(matcher.match(44));
  }
}
