package org.opentripplanner.transit.model.filter.expr;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NullSafeWrapperMatcherTest {

  @Test
  void testMatches() {
    var matcher = new NullSafeWrapperMatcher<>(
      "string",
      s -> s,
      new CaseInsensitiveStringPrefixMatcher<>("string", "namePrefix", s -> s.toString())
    );
    assertTrue(matcher.match("namePrefix and more"));
    assertFalse(matcher.match("not namePrefix"));
    assertFalse(matcher.match(null));
  }

  @Test
  void testFailsWithoutNullSafeWrapperMatcher() {
    var matcher = new CaseInsensitiveStringPrefixMatcher<>("string", "here's a string", s ->
      s.toString()
    );
    assertThrows(NullPointerException.class, () -> matcher.match(null));
  }
}
