package org.opentripplanner.transit.raptor.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PathStringBuilderTest {
    private static final String MODE = "BUS";
    private static final int T_10_46_05 = time(10, 46, 5);
    private static final int T_10_55 = time(10, 55, 0);
    private static final int D_5_12 = time(0, 5, 12);


    @Test
    public void walk() {
        assertEquals("Walk 5m12s", new PathStringBuilder().walk(D_5_12).toString());
        assertEquals("Walk 17s", new PathStringBuilder().walk(17).toString());
        assertEquals("Walk   17s", new PathStringBuilder(true).walk(17).toString());
    }

    @Test
    public void transit() {
        assertEquals("BUS 10:46:05 10:55", new PathStringBuilder().transit(MODE, T_10_46_05, T_10_55).toString());
    }

    @Test
    public void stop() {
        assertEquals("5000", new PathStringBuilder().stop(5000).toString());
    }

    @Test
    public void sep() {
        assertEquals(" ~ ", new PathStringBuilder().sep().toString());
    }

    @Test
    public void path() {
        assertEquals(
            "Walk 37s ~ 227 ~ BUS 10:46:05 10:55 ~ 112 ~ Walk 1h37m7s",
            new PathStringBuilder()
                        .walk(37).sep().stop(227).sep()
                        .transit(MODE, T_10_46_05, T_10_55).sep().stop(112).sep()
                        .walk(3600 + 37 * 60 + 7).toString()
        );
    }

    /* privet methods */

    private static int time(int hour, int min, int sec) {
        return 3600 * hour + 60 * min + sec;
    }
}