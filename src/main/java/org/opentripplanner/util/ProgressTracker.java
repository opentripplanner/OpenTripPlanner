package org.opentripplanner.util;

import java.util.function.BiConsumer;


/**
 * The progress tracker notify the caller based on 'time' and a 'counter'.
 * <p>
 * The 'counter' is used to notify the caller for each percent(1%) it the total number of
 * steps is more than 100. If the number steps is less than 100 the caller is notified every time.
 * <p>
 * To avoid the caller from being notified to often, the tracker also uses a 'timer'. The
 * 'timer' prevent notification unless a minimum amount of time is passed since last time the
 * caller was notified.
 * <p>
 * Both the 'counter' and the 'timer' constraint must pass for the caller to be notified.
 */
public class ProgressTracker {
    private final int dStep;
    private final int size;
    private final long notifyEveryMillisecond;

    private int i = 0;
    private long lastProgressNotification;

    public ProgressTracker(int size, long notifyEveryMillisecond) {
        this.size = size;
        this.dStep = Math.max(size / 100, 1);
        this.notifyEveryMillisecond = notifyEveryMillisecond;
        this.lastProgressNotification = System.currentTimeMillis();
    }

    public void step(BiConsumer<Integer, Integer> progressNotification) {
        ++i;

        // Another 1% of the steps are complete
        if(i % dStep != 0) { return; }

        // And it is more than N milliseconds since last notification
        long time = System.currentTimeMillis();
        if(time < lastProgressNotification + notifyEveryMillisecond) { return; }

        // The notify
        lastProgressNotification = time;
        progressNotification.accept(i, size);
    }
}
