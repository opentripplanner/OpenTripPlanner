package org.opentripplanner.util.logging;

import org.slf4j.Logger;


/**
 * This class can be used to log N logging events with level:
 * <ul>
 *     <li>INFO</li>
 *     <li>WARNING</li>
 *     <li>ERROR</li>
 * </ul>
 * DEBUG and TRACE events are not muted.
 * <p>
 * The primary use-case for this class is to prevent a logger form spamming the log with the same
 * message. After a given limit this logger will be muted and no more log events are logged.
 * <p>
 * THREAD SAFETY - The implementation is not thread safe.
 */
public class MaxCountLogger extends AbstractFilterLogger {

    private static final int MAX_COUNT = 10;
    private int count = 0;

    public MaxCountLogger(Logger delegate) {
        super(delegate);
    }

    /**
     * Wrap given logger, and throttle INFO, WARN and ERROR messages.
     */
    public static MaxCountLogger maxCount(Logger log) {
        return new MaxCountLogger(log);
    }

    @Override
    boolean mute() {
        ++count;
        return count > MAX_COUNT;
    }

    /**
     * Log the total number of log events if at least one event was muted. The log text is
     * formatted like this: {@code "TOTAL: n - %message%" }.
     */
    public void logTotal(String message) {
        if(mute()) {
            getDelegate().warn("TOTAL: {} - {}", count, message);
        }
    }
}
