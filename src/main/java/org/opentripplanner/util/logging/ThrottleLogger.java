package org.opentripplanner.util.logging;

import org.slf4j.Logger;


/**
 * This class can be used to throttle logging events with level:
 * <ul>
 *     <li>INFO</li>
 *     <li>WARNING</li>
 *     <li>ERROR</li>
 * </ul>
 * DEBUG and TRACE events are not throttled.
 * <p>
 * The primary use-case for this class is to prevent a logger for degrading the performance,
 * because too many events are logged during a short period of time. This could happen if you are
 * parsing thousands or millions of records and each of them will cause a log event to happen.
 * <p>
 * This class is used to wrap the original logger and it will forward only one log event for pr
 * second.
 * <p>
 * THREAD SAFETY - The implementation is very simple and do not do any synchronization, so it is
 * possible that more than 1 log event is logged for each second, but that is the only thread
 * safety issue. It is safe to use in multi-threaded cases. See the JavaDoc on the private
 * {@code throttle()} method for implementation details.
 */
public class ThrottleLogger extends AbstractFilterLogger {

    private static final int STALL_PERIOD_MILLISECONDS = 1000;
    private long timeout = Long.MIN_VALUE;

    private ThrottleLogger(Logger delegate) {
        super(delegate);
        delegate.info(
                "Logger {} is throttled, only one messages is logged for every {} second interval.",
                delegate.getName(), STALL_PERIOD_MILLISECONDS / 1000
        );
    }

    /**
     * Wrap given logger, and throttle INFO, WARN and ERROR messages.
     */
    public static Logger throttle(Logger log) {
        return new ThrottleLogger(log);
    }

    @Override
    boolean mute() {
        return throttle();
    }

    /**
     * This method check if the throttle timeout is set and return {@code true} if it is. It also
     * set the next timeout. The write/read operations are NOT synchronized witch may cause two or
     * more concurrent calls to both return {@code false}, hence causing two log events for the same
     * throttle time period - witch is a minor drawback. The throttle do however guarantee that at
     * least one event is logged for each throttle time period. This is guaranteed based on the
     * assumption that writing to the {@code timeout} (primitive long) is an atomic operation.
     * <p>
     * In a worst case scenario, each thread keep their local version of the {@code timeout} and
     * one log message from each thread is printed every second. This can behave differently
     * from one JVM to anther.
     */
    private boolean throttle() {
        long time = System.currentTimeMillis();

        if (time < timeout) {
            return true;
        }
        timeout = time + STALL_PERIOD_MILLISECONDS;
        return false;
    }
}
