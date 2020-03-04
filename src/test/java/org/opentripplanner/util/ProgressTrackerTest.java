package org.opentripplanner.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProgressTrackerTest {
    private int i = -1;
    private int size = -1;
    private boolean breakOut = false;


    @Test
    public void testStepLessThan100() {
        ProgressTracker p = new ProgressTracker(30, 0);

        p.step((c, s) -> { i = c; size = s;});
        assertEquals(1, i);
        assertEquals(30, size);

        p.step((c, s) -> { i = c; size = s;});
        assertEquals(2, i);
        assertEquals(30, size);
    }

    @Test
    public void testStepMoreThan100() {
        ProgressTracker p = new ProgressTracker(200, 0);

        p.step((c, s) -> { i = c; size = s;});
        assertEquals(-1, i);
        assertEquals(-1, size);

        p.step((c, s) -> { i = c; size = s;});
        assertEquals(2, i);
        assertEquals(200, size);
    }

    @Test
    public void testStepEvery20ms() {
        // We want to track the progress every 100 ms
        long WAIT = 100;
        long start = System.currentTimeMillis();

        // and if the test takes more than 200 millisec we want to abort the test
        long endFuse = start + 2 * WAIT;

        // When tracking every step, but not more often than WAIT time
        ProgressTracker subject = new ProgressTracker(1, WAIT);
        int i = 0;

        while (!breakOut && System.currentTimeMillis() < endFuse) {
            sleep(WAIT/10);
            subject.step((c,size) -> breakOut = true);
            ++i;
        }

        // Then the expected duration of the test should be around the WAIT time
        long time = System.currentTimeMillis() - start;
        assertTrue(
                "Time should be close to the wait time: " + WAIT + " but is " + time,
                Math.abs(time - WAIT) < WAIT
        );
        assertTrue(
                "We expect the loop to be performed between 5 and 12 times: " + i,
                i > 5 && i < 12
        );
    }

    void sleep(long timeMs) {
        try { Thread.sleep(timeMs); }
        catch (InterruptedException e) { throw new RuntimeException(e.getMessage(), e); }
    }
}