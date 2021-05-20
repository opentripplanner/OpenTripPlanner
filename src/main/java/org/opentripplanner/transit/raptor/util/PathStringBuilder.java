package org.opentripplanner.transit.raptor.util;

import java.util.Calendar;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.util.time.DurationUtils;
import org.opentripplanner.util.time.TimeUtils;

/**
 * Create a path like: {@code Walk 5m - 101 - Transit 10:07 10:35 - 2111 - Walk 4m }
 */
public class PathStringBuilder {
    private final StringBuilder buf = new StringBuilder();
    private final boolean padDuration;
    private boolean elementAdded = false;
    private boolean sepAdded = false;

    public PathStringBuilder() {
        this(false);
    }
    public PathStringBuilder(boolean padDuration) {
        this.padDuration = padDuration;
    }

    public PathStringBuilder sep() {
        sepAdded = true;
        return this;
    }

    public PathStringBuilder stop(int stop) {
        return start().append(stop).end();
    }

    public PathStringBuilder stop(String stop) {
        return start().append(stop).end();
    }

    public PathStringBuilder walk(int duration) {
        return start().append("Walk").duration(duration).end();
    }

    public PathStringBuilder flex(int duration, int nRides) {
        // The 'tx' is short for eXtra Transfers added by the flex access/egress.
        return start().append("Flex").duration(duration).space().append(nRides).append("tx").end();
    }

    public PathStringBuilder accessEgress(RaptorTransfer leg) {
        if(leg.hasRides()) {
            return flex(leg.durationInSeconds(), leg.numberOfRides());
        }
        return leg.durationInSeconds() == 0 ? this : walk(leg.durationInSeconds());
    }

    public PathStringBuilder transit(String description, int fromTime, int toTime) {
        return start().append(description).space().time(fromTime, toTime).end();
    }

    public PathStringBuilder transit(
            TraverseMode mode, String trip, Calendar fromTime, Calendar toTime
    ) {
        return start()
                .append(mode.name()).space()
                .append(trip).space()
                .time(fromTime, toTime)
                .end();
    }

    public PathStringBuilder other(TraverseMode mode, Calendar fromTime, Calendar toTime) {
        return start().append(mode.name()).space().time(fromTime, toTime).end();
    }

    public PathStringBuilder timeAndCost(int fromTime, int toTime, int generalizedCost) {
        return space().time(fromTime, toTime).cost(generalizedCost);
    }

    public PathStringBuilder cost(int generalizedCost) {
        if(generalizedCost> 0) {
            space().append("$").append(generalizedCost);
        }
        return this;
    }

    public PathStringBuilder space() {
        return append(" ");
    }

    public PathStringBuilder duration(int duration) {
        String durationStr = DurationUtils.durationToStr(duration);
        return space().append(padDuration ? String.format("%5s", durationStr) : durationStr);
    }

    public PathStringBuilder time(int from, int to) {
        return append(TimeUtils.timeToStrCompact(from))
                .space()
                .append(TimeUtils.timeToStrCompact(to));
    }

    public PathStringBuilder append(String text) {
        buf.append(text);
        return this;
    }


    @Override
    public String toString() {
        return buf.toString();
    }

    /* private helpers */

    private PathStringBuilder time(Calendar from, Calendar to) {
        return append(TimeUtils.timeToStrCompact(from))
            .space()
            .append(TimeUtils.timeToStrCompact(to));
    }

    private PathStringBuilder append(int value) {
        buf.append(value);
        return this;
    }

    private PathStringBuilder end() {
        elementAdded = true;
        sepAdded = false;
        return this;
    }

    private PathStringBuilder start() {
        if(sepAdded && elementAdded) {
            append(" ~ ");
        }
        elementAdded = false;
        return this;
    }
}
