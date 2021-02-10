package org.opentripplanner.transit.raptor.util;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.util.time.DurationUtils;
import org.opentripplanner.util.time.TimeUtils;

import java.util.Calendar;

/**
 * Create a path like: {@code Walk 5m - 101 - Transit 10:07 10:35 - 2111 - Walk 4m }
 */
public class PathStringBuilder {
    private final StringBuilder buf = new StringBuilder();
    private final boolean padDuration;

    public PathStringBuilder() {
        this(false);
    }
    public PathStringBuilder(boolean padDuration) {
        this.padDuration = padDuration;
    }

    public PathStringBuilder sep() {
        return append(" ~ ");
    }

    public PathStringBuilder stop(int stop) {
        return append(stop);
    }

    public PathStringBuilder stop(String stop) {
        return append(stop);
    }

    public PathStringBuilder walk(int duration) {
        return append("Walk ").duration(duration);
    }

    public PathStringBuilder flex(int duration, int nRides) {
        // The 'tx' is short for eXtra Transfers added by the flex access/egress.
        return append("Flex ").duration(duration).space().append(nRides).append("tx");
    }

    public PathStringBuilder accessEgress(RaptorTransfer leg) {
        if(leg.hasRides()) {
            return flex(leg.durationInSeconds(), leg.numberOfRides());
        }
        return walk(leg.durationInSeconds());
    }

    public PathStringBuilder transit(String description, int fromTime, int toTime) {
        return append(description).space().time(fromTime, toTime);
    }

    public PathStringBuilder transit(TraverseMode mode, String trip, Calendar fromTime, Calendar toTime) {
        return append(mode.name()).space().append(trip).space().time(fromTime, toTime);
    }

    public PathStringBuilder other(TraverseMode mode, Calendar fromTime, Calendar toTime) {
        return append(mode.name()).space().time(fromTime, toTime);
    }

    @Override
    public String toString() {
        return buf.toString();
    }

    /* private helpers */

    private PathStringBuilder space() {
        return append(" ");
    }

    private PathStringBuilder duration(int duration) {
      String durationStr = DurationUtils.durationToStr(duration);
        return append(padDuration ? String.format("%5s", durationStr) : durationStr);
    }

    private PathStringBuilder time(int from, int to) {
        return append(TimeUtils.timeToStrCompact(from))
                .space()
                .append(TimeUtils.timeToStrCompact(to));
    }

    private PathStringBuilder time(Calendar from, Calendar to) {
        return append(TimeUtils.timeToStrCompact(from))
            .space()
            .append(TimeUtils.timeToStrCompact(to));
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
