package org.opentripplanner.routing.core;

import junit.framework.TestCase;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;

public class TestStopMatcher extends TestCase {

    /**
     * Test different stop matchers
     */
    public void testStopMatcher() {
        Stop s1 = new Stop();
        s1.setId(new FeedScopedId("A1", "42"));
        Stop s2 = new Stop();
        s2.setId(new FeedScopedId("A1", "43"));

        StopMatcher emptyMatcher = StopMatcher.emptyMatcher();
        assertFalse(emptyMatcher.matches(s1));
        assertFalse(emptyMatcher.matches(s2));

        StopMatcher matcherS1 = StopMatcher.parse("A1:42");
        assertTrue(matcherS1.matches(s1));
        assertFalse(matcherS1.matches(s2));

        StopMatcher matcherS2 = StopMatcher.parse("A1:43");
        assertFalse(matcherS2.matches(s1));
        assertTrue(matcherS2.matches(s2));

        StopMatcher matcherS1S2 = StopMatcher.parse("A1:42,A1:43");
        assertTrue(matcherS1S2.matches(s1));
        assertTrue(matcherS1S2.matches(s2));

        StopMatcher nullList = StopMatcher.parse(null);
        assertTrue(nullList.isEmpty());

        StopMatcher emptyList = StopMatcher.parse("");
        assertTrue(emptyList.isEmpty());

        StopMatcher degenerate = StopMatcher.parse(",,,");
        assertTrue(degenerate.isEmpty());

        boolean thrown = false;
        try {
            @SuppressWarnings("unused")
            StopMatcher badMatcher = StopMatcher.parse("A1");
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    /**
     * Test different stop matchers including stops with parents
     */
    public void testStopMatcherParents() {
        Stop parent = new Stop();
        parent.setId(new FeedScopedId("A1", "10"));
        Stop s1 = new Stop();
        s1.setId(new FeedScopedId("A1", "42"));
        s1.setParentStation("10");
        Stop s2 = new Stop();
        s2.setId(new FeedScopedId("A1", "43"));
        s2.setParentStation("10");
        
        StopMatcher matcherParent = StopMatcher.parse("A1:10");
        assertTrue(matcherParent.matches(parent));
        assertTrue(matcherParent.matches(s1));
        assertTrue(matcherParent.matches(s2));
        
        StopMatcher matcherS1 = StopMatcher.parse("A1:42");
        assertFalse(matcherS1.matches(parent));
        assertTrue(matcherS1.matches(s1));
        assertFalse(matcherS1.matches(s2));
        
        StopMatcher matcherS2 = StopMatcher.parse("A1:43");
        assertFalse(matcherS2.matches(parent));
        assertFalse(matcherS2.matches(s1));
        assertTrue(matcherS2.matches(s2));
        
        StopMatcher matcherS1S2 = StopMatcher.parse("A1:42,A1:43");
        assertFalse(matcherS1S2.matches(parent));
        assertTrue(matcherS1S2.matches(s1));
        assertTrue(matcherS1S2.matches(s2));
    }
}
