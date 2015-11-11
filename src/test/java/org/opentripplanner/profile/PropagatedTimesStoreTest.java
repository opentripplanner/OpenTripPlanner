package org.opentripplanner.profile;

import junit.framework.TestCase;
import org.junit.Test;
import org.opentripplanner.graph_builder.module.FakeGraph;
import org.opentripplanner.routing.graph.Graph;

/**
 * Test the propagated times store.
 */
public class PropagatedTimesStoreTest extends TestCase {
    /**
     * Test that changing the reachability threshold works (i.e. averages are computed properly when destinations are
     * only reachable part of the time).
     */
    @Test
    public static void testReachability () throws Exception {
        ProfileRequest pr = new ProfileRequest();
        // old default: no restrictions o
        pr.reachabilityThreshold = 0;

        Graph g = new Graph();

        PropagatedTimesStore pts = new PropagatedTimesStore(g, pr, 1);
        // accessible one-third of the time
        int[][] times = new int[][] {
                new int[] { 1 },
                new int[] { RaptorWorker.UNREACHED },
                new int[] { RaptorWorker.UNREACHED }
        };

        pts.setFromArray(times, new boolean[] { true, true, true }, PropagatedTimesStore.ConfidenceCalculationMethod.MIN_MAX);

        // it is reachable at least 0% of the time
        assertEquals(1, pts.avgs[0]);

        pr.reachabilityThreshold = 0.5f;
        pts = new PropagatedTimesStore(g, pr, 1);
        pts.setFromArray(times, new boolean[] {true, true, true}, PropagatedTimesStore.ConfidenceCalculationMethod.MIN_MAX);

        // it is not reachable 50% of the time
        assertEquals(RaptorWorker.UNREACHED, pts.avgs[0]);
    }

    /**
     * Test that adjusting which numbers are included in averages works.
     * We calculate the min and max for frequency lines, and we don't include the extrema when we do the average.
     */
    @Test
    public static void testAverageInclusion () {
        ProfileRequest pr = new ProfileRequest();
        Graph g = new Graph();

        int[][] times = new int[][] {
                new int[] {1},
                new int[] {1000}
        };

        PropagatedTimesStore pts = new PropagatedTimesStore(g, pr, 1);
        pts.setFromArray(times, new boolean[] { true, true }, PropagatedTimesStore.ConfidenceCalculationMethod.MIN_MAX);
        // average of 1 and 1000 is 500 (int math)
        assertEquals(500, pts.avgs[0]);

        pts = new PropagatedTimesStore(g, pr, 1);
        pts.setFromArray(times, new boolean[] { true, false }, PropagatedTimesStore.ConfidenceCalculationMethod.MIN_MAX);
        // 1000 should not be included in average
        assertEquals(1, pts.avgs[0]);
    }
}