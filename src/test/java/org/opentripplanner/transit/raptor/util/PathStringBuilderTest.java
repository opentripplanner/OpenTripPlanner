package org.opentripplanner.transit.raptor.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PathStringBuilderTest {
    private static final int T_10_46 = time(10, 46, 5);
    private static final int T_10_55 = time(10, 55, 0);
    private static final int D_5_12 = time(0, 5, 12);


    @Test
    public void walk() {
        assertPath("Walk 5m12s", new PathStringBuilder().walk(D_5_12));
    }

    @Test
    public void transit() {
        assertPath("Transit 10:46 10:55", new PathStringBuilder().transit(T_10_46, T_10_55));
    }

    @Test
    public void stop() {
        assertPath("5000", new PathStringBuilder().stop(5000));
    }

    @Test
    public void sep() {
        assertPath(" - ", new PathStringBuilder().sep());
    }

    @Test
    public void path() {
        assertPath(
                "Walk 37s - 227 - Transit 10:46 10:55 - 112 - Walk 1h37m7s",
                new PathStringBuilder()
                        .walk(37).sep().stop(227).sep()
                        .transit(T_10_46, T_10_55).sep().stop(112).sep()
                        .walk(3600 + 37 * 60 + 7)
        );
    }


    /* privet methods */

    private void assertPath(String expected, PathStringBuilder path) {
        assertEquals(expected, path.toString());
    }

    private static int time(int hour, int min, int sec) {
        return 60 * (60 * hour + min) + sec;
    }

}