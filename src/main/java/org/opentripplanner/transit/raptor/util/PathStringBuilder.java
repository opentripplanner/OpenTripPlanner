package org.opentripplanner.transit.raptor.util;

/**
 * Create a path like: {@code Walk 5m - 101 - Transit 10:07 10:35 - 2111 - Walk 4m }
 */
public class PathStringBuilder {
    private StringBuilder buf = new StringBuilder();

    public PathStringBuilder sep() {
        return append(" - ");
    }

    public PathStringBuilder stop(int stop) {
        return append(stop);
    }

    public PathStringBuilder walk(int duration) {
        return append("Walk ").duration(duration);
    }

    public PathStringBuilder transit(int fromTime, int toTime) {
        return append("Transit ").time(fromTime, toTime);
    }

    @Override
    public String toString() {
        return buf.toString();
    }

    /* private helpers */

    private PathStringBuilder duration(int duration) {
        return append(TimeUtils.durationToStr(duration));
    }

    private PathStringBuilder time(int from, int to) {
        return append(TimeUtils.timeToStrShort(from)).append(" ").append(TimeUtils.timeToStrShort(to));
    }

    private PathStringBuilder append(String text) {
        buf.append(text);
        return this;
    }

    private PathStringBuilder append(int value) {
        buf.append(value);
        return this;
    }
}
