package org.opentripplanner.transit.model.filter.expr;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CaseInsensitiveStringPrefixMatcherTest {

  @Test
  void testMatches() {
    var matcher = new CaseInsensitiveStringPrefixMatcher<>("prefix", "foo", s -> s.toString());
    assertTrue(matcher.match("foo"));
    assertTrue(matcher.match("foobar"));
    assertFalse(matcher.match("bar"));
  }
}
