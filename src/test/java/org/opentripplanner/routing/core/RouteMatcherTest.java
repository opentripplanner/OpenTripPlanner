package org.opentripplanner.routing.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TransitModelForTest.route;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;

public class RouteMatcherTest {

  @Test
  public void testRouteMatcher() {
    Route r1 = route("X").setId(new FeedScopedId("A1", "42")).withShortName("R1").build();
    Route r2 = route("X").setId(new FeedScopedId("A1", "43")).withShortName("R2").build();
    Route r1b = route("X").setId(new FeedScopedId("A2", "42")).withShortName("R1").build();
    Route r3 = route("X").setId(new FeedScopedId("A1", "44")).withShortName("R3_b").build();

    RouteMatcher emptyMatcher = RouteMatcher.emptyMatcher();
    assertFalse(emptyMatcher.matches(r1));
    assertFalse(emptyMatcher.matches(r1b));
    assertFalse(emptyMatcher.matches(r2));

    RouteMatcher matcherR1i = RouteMatcher.parse("A1__42");
    assertTrue(matcherR1i.matches(r1));
    assertFalse(matcherR1i.matches(r1b));
    assertFalse(matcherR1i.matches(r2));

    RouteMatcher matcherR2n = RouteMatcher.parse("A1_R2");
    assertFalse(matcherR2n.matches(r1));
    assertFalse(matcherR2n.matches(r1b));
    assertTrue(matcherR2n.matches(r2));

    RouteMatcher matcherR1R2 = RouteMatcher.parse("A1_R1,A1__43,A2__43");
    assertTrue(matcherR1R2.matches(r1));
    assertFalse(matcherR1R2.matches(r1b));
    assertTrue(matcherR1R2.matches(r2));

    RouteMatcher matcherR1n = RouteMatcher.parse("_R1");
    assertTrue(matcherR1n.matches(r1));
    assertTrue(matcherR1n.matches(r1b));
    assertFalse(matcherR1n.matches(r2));

    RouteMatcher matcherR1R1bR2 = RouteMatcher.parse("A1_R1,A2_R1,A1_R2");
    assertTrue(matcherR1R1bR2.matches(r1));
    assertTrue(matcherR1R1bR2.matches(r1b));
    assertTrue(matcherR1R1bR2.matches(r2));

    RouteMatcher matcherR3e = RouteMatcher.parse("A1_R3 b");
    assertFalse(matcherR3e.matches(r1));
    assertFalse(matcherR3e.matches(r1b));
    assertFalse(matcherR3e.matches(r2));
    assertTrue(matcherR3e.matches(r3));

    RouteMatcher nullList = RouteMatcher.parse(null);
    assertSame(nullList, RouteMatcher.emptyMatcher());

    RouteMatcher emptyList = RouteMatcher.parse("");
    assertSame(emptyList, RouteMatcher.emptyMatcher());

    RouteMatcher degenerate = RouteMatcher.parse(",,,");
    assertSame(degenerate, RouteMatcher.emptyMatcher());

    boolean thrown = false;
    try {
      RouteMatcher badMatcher = RouteMatcher.parse("A1_R1_42");
    } catch (IllegalArgumentException e) {
      thrown = true;
    }
    assertTrue(thrown);

    Route r1c = TransitModelForTest
      .route("X")
      .setId(new FeedScopedId("A_1", "R_42"))
      .withShortName("R_42")
      .build();

    RouteMatcher matcherR1c = RouteMatcher.parse("A\\_1_R 42");
    assertTrue(matcherR1c.matches(r1c));
    assertFalse(matcherR1c.matches(r1));
    assertFalse(matcherR1c.matches(r1b));

    RouteMatcher matcherR1c2 = RouteMatcher.parse("A\\_1__R\\_42");
    assertTrue(matcherR1c2.matches(r1c));
    assertFalse(matcherR1c2.matches(r1));
    assertFalse(matcherR1c2.matches(r1b));
  }
}
