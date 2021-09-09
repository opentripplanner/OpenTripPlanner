package org.opentripplanner.util.logging;

import org.slf4j.Logger;
import org.slf4j.Marker;


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
public class ThrottleLogger implements Logger {

    private static final int STALL_PERIOD_MILLISECONDS = 1000;
    private final Logger delegate;
    private long timeout = Long.MIN_VALUE;

    private ThrottleLogger(Logger delegate) {
        this.delegate = delegate;
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
    public String getName() {
        return delegate.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return delegate.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        delegate.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        delegate.trace(format, arg);

    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        delegate.trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        delegate.trace(format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        delegate.trace(msg, t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return delegate.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String msg) {
        delegate.trace(marker, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        delegate.trace(marker, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        delegate.trace(marker, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        delegate.trace(marker, format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        delegate.trace(marker, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return delegate.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        delegate.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        delegate.debug(format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        delegate.debug(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        delegate.debug(format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        delegate.debug(msg, t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return delegate.isTraceEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String msg) {
        delegate.debug(marker, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        delegate.debug(marker, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        delegate.debug(marker, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        delegate.debug(marker, format, arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        delegate.debug(marker, msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return delegate.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        if (throttle()) { return; }
        delegate.info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        if (throttle()) { return; }
        delegate.info(format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        if (throttle()) { return; }
        delegate.info(format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        if (throttle()) { return; }
        delegate.info(format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        if (throttle()) { return; }
        delegate.info(msg, t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return delegate.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        if (throttle()) { return; }
        delegate.info(marker, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        if (throttle()) { return; }
        delegate.info(marker, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        if (throttle()) { return; }
        delegate.info(marker, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        if (throttle()) { return; }
        delegate.info(marker, format, arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        if (throttle()) { return; }
        delegate.info(marker, msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return delegate.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        if (throttle()) { return; }
        delegate.warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        if (throttle()) { return; }
        delegate.warn(format, arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        if (throttle()) { return; }
        delegate.warn(format, arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        if (throttle()) { return; }
        delegate.warn(format, arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        if (throttle()) { return; }
        delegate.warn(msg, t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return delegate.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        if (throttle()) { return; }
        delegate.warn(marker, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        if (throttle()) { return; }
        delegate.warn(marker, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        if (throttle()) { return; }
        delegate.warn(marker, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        if (throttle()) { return; }
        delegate.warn(marker, format, arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        if (throttle()) { return; }
        delegate.warn(marker, msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return delegate.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        if (throttle()) { return; }
        delegate.error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        if (throttle()) { return; }
        delegate.error(format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        if (throttle()) { return; }
        delegate.error(format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        if (throttle()) { return; }
        delegate.error(format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        if (throttle()) { return; }
        delegate.error(msg, t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return delegate.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg) {
        if (throttle()) { return; }
        delegate.error(marker, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        if (throttle()) { return; }
        delegate.error(marker, format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        if (throttle()) { return; }
        delegate.error(marker, format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        if (throttle()) { return; }
        delegate.error(marker, format, arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        if (throttle()) { return; }
        delegate.error(marker, msg, t);
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
