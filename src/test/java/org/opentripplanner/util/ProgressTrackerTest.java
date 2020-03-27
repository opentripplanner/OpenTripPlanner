package org.opentripplanner.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProgressTrackerTest {
    private String msg;
    private boolean breakOut = false;


    @Test
    public void testStepLessThan100() {
        ProgressTracker p = new ProgressTracker("Pete", 1,-1, 0, false);

        msg = null;
        p.step(m -> msg = m);
        assertEquals("Pete progress: 1 done", msg);

        msg = null;
        p.step(m -> msg = m);
        assertEquals("Pete progress: 2 done", msg);

        msg = p.completeMessage();
        assertTrue(msg, msg.startsWith("Pete progress tracking complete. 2 done in"));
    }

    @Test
    public void testStepMoreThan100() {
        ProgressTracker p = new ProgressTracker("Pete", 2,200, 0, true);

        assertEquals("Pete progress tracking started.", p.startMessage());

        msg = null;
        p.step(m -> msg = m);
        assertNull(msg, msg);

        msg = null;
        p.step(m -> msg = m);
        assertEquals("Pete progress: 2 bytes of 200 bytes ( 1%)", msg);

        msg = p.completeMessage();
        assertTrue(msg, msg.startsWith("Pete progress tracking complete. 2 bytes done in"));
    }

    @Test
    public void testStepEvery20ms() {
        // We want to track the progress every 100 ms
        long WAIT = 100;
        long start = System.currentTimeMillis();

        // and if the test takes more than 200 millisec we want to abort the test
        long endFuse = start + 2 * WAIT;

        // When tracking every step, but not more often than WAIT time
        ProgressTracker subject = new ProgressTracker("Pete", 1, -1, WAIT, false);
        int i = 0;

        while (!breakOut && System.currentTimeMillis() < endFuse) {
            sleep(WAIT/10);
            subject.step(m -> breakOut = true);
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