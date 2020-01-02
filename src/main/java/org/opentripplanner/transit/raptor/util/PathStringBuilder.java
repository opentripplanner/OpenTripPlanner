package org.opentripplanner.transit.raptor.util;

/**
 * Create a path like: {@code walk 5:12 > 34567 > transit 12:10-12:50 > 23456 > walk 4:01 }
 */
public class PathStringBuilder {
    private StringBuilder buf = new StringBuilder();

    // TODO TGR - Replace '>' with '-' better readability
    public org.opentripplanner.transit.raptor.util.PathStringBuilder sep() {
        return append(" > ");
    }

    public org.opentripplanner.transit.raptor.util.PathStringBuilder stop(int stop) {
        return append(stop);
    }

    public org.opentripplanner.transit.raptor.util.PathStringBuilder walk(int duration) {
        return append("Walk ").duration(duration);
    }

    public org.opentripplanner.transit.raptor.util.PathStringBuilder transit(int fromTime, int toTime) {
        return append("Transit ").time(fromTime, toTime);
    }

    @Override
    public String toString() {
        return buf.toString();
    }

    /* private helpers */

    private org.opentripplanner.transit.raptor.util.PathStringBuilder duration(int duration) {
        return append(TimeUtils.timeToStrCompact(duration));
    }

    // TODO TGR - Replace '-' with ' ' better readability
    private org.opentripplanner.transit.raptor.util.PathStringBuilder time(int from, int to) {
        return append(TimeUtils.timeToStrShort(from)).append("-").append(TimeUtils.timeToStrShort(to));
    }

    private org.opentripplanner.transit.raptor.util.PathStringBuilder append(String text) {
        buf.append(text);
        return this;
    }

    private org.opentripplanner.transit.raptor.util.PathStringBuilder append(int value) {
        buf.append(value);
        return this;
    }
}
