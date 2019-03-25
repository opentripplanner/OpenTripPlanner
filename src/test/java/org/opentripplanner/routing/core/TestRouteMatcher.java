package org.opentripplanner.routing.core;

import junit.framework.TestCase;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;

public class TestRouteMatcher extends TestCase {

    public void testRouteMatcher() {

        Route r1 = new Route();
        r1.setId(new FeedScopedId("A1", "42"));
        r1.setShortName("R1");
        Route r2 = new Route();
        r2.setId(new FeedScopedId("A1", "43"));
        r2.setShortName("R2");
        Route r1b = new Route();
        r1b.setId(new FeedScopedId("A2", "42"));
        r1b.setShortName("R1");
        Route r3 = new Route();
        r3.setId(new FeedScopedId("A1", "44"));
        r3.setShortName("R3_b");

        RouteMatcher emptyMatcher = RouteMatcher.emptyMatcher();
        assertFalse(emptyMatcher.matches(r1));
        assertFalse(emptyMatcher.matches(r1b));
        assertFalse(emptyMatcher.matches(r2));

        RouteMatcher matcherR1i = RouteMatcher.emptyMatcher();
        matcherR1i.addRoutes("A1__42");
        assertTrue(matcherR1i.matches(r1));
        assertFalse(matcherR1i.matches(r1b));
        assertFalse(matcherR1i.matches(r2));

        RouteMatcher matcherR2n = RouteMatcher.emptyMatcher();
        matcherR2n.addRoutes("A1_R2");
        assertFalse(matcherR2n.matches(r1));
        assertFalse(matcherR2n.matches(r1b));
        assertTrue(matcherR2n.matches(r2));

        RouteMatcher matcherR1R2 = RouteMatcher.emptyMatcher();
        matcherR1R2.addRoutes("A1_R1,A1__43,A2__43");
        assertTrue(matcherR1R2.matches(r1));
        assertFalse(matcherR1R2.matches(r1b));
        assertTrue(matcherR1R2.matches(r2));

        RouteMatcher matcherR1n = RouteMatcher.emptyMatcher();
        matcherR1n.addRoutes("_R1");
        assertTrue(matcherR1n.matches(r1));
        assertTrue(matcherR1n.matches(r1b));
        assertFalse(matcherR1n.matches(r2));

        RouteMatcher matcherR1R1bR2 = RouteMatcher.emptyMatcher();
        matcherR1R1bR2.addRoutes("A1_R1,A2_R1,A1_R2");
        assertTrue(matcherR1R1bR2.matches(r1));
        assertTrue(matcherR1R1bR2.matches(r1b));
        assertTrue(matcherR1R1bR2.matches(r2));

        RouteMatcher matcherR3e = RouteMatcher.emptyMatcher();
        matcherR3e.addRoutes("A1_R3 b");
        assertFalse(matcherR3e.matches(r1));
        assertFalse(matcherR3e.matches(r1b));
        assertFalse(matcherR3e.matches(r2));
        assertTrue(matcherR3e.matches(r3));

        RouteMatcher nullList = RouteMatcher.emptyMatcher();
        nullList.addRoutes(null);
        assertTrue(nullList.equals(RouteMatcher.emptyMatcher()));

        RouteMatcher emptyList = RouteMatcher.emptyMatcher();
        emptyList.addRoutes("");
        assertTrue(emptyList.equals(RouteMatcher.emptyMatcher()));

        RouteMatcher degenerate = RouteMatcher.emptyMatcher();
        degenerate.addRoutes(",,,");
        assertTrue(degenerate.equals(RouteMatcher.emptyMatcher()));


        RouteMatcher badMatcher = RouteMatcher.emptyMatcher();
        badMatcher.addRoutes("A1_R1_42");
        assertTrue(badMatcher.isEmpty());

        Route r1c = new Route();
        r1c.setId(new FeedScopedId("A_1", "R_42"));
        r1c.setShortName("R_42");

        RouteMatcher matcherR1c = RouteMatcher.emptyMatcher();
        matcherR1c.addRoutes("A\\_1_R 42");
        assertTrue(matcherR1c.matches(r1c));
        assertFalse(matcherR1c.matches(r1));
        assertFalse(matcherR1c.matches(r1b));

        RouteMatcher matcherR1c2 = RouteMatcher.emptyMatcher();
        matcherR1c2.addRoutes("A\\_1__R\\_42");
        assertTrue(matcherR1c2.matches(r1c));
        assertFalse(matcherR1c2.matches(r1));
        assertFalse(matcherR1c2.matches(r1b));
    }

}
