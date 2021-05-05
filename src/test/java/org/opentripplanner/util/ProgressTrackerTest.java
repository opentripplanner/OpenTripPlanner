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
        assertNull("Pete progress: 2 bytes of 200 bytes ( 1%)", msg);

        msg = null;
        p.step(m -> msg = m);
        assertEquals("Pete progress: 2 bytes of 200 bytes ( 1%)", msg);

        msg = p.completeMessage();
        assertTrue(msg, msg.startsWith("Pete progress tracking complete. 2 bytes done in"));
    }

    @Test
    public void testNoOutputInQuietPeriod() {
        long QUIET_PERIOD = 1000;
        ProgressTracker subject = new ProgressTracker("Pete", 1,100, QUIET_PERIOD, true);
        long start = System.currentTimeMillis();

        sleep10ms();
        subject.step(m -> breakOut = true);

        sleep10ms();
        subject.step(m -> breakOut = true);

        long time = System.currentTimeMillis() - start;
        // If test was able to run within the quiet period
        if(time < QUIET_PERIOD) {
            assertFalse(
                    "No steps should log anything within the quiet period. Time: " + time,
                    breakOut
            );
        }
    }

    void sleep10ms() {
        try { Thread.sleep(10); }
        catch (InterruptedException e) { throw new RuntimeException(e.getMessage(), e); }
    }
}