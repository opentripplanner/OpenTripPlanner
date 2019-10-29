package org.opentripplanner.transit.raptor.api.path;

import org.opentripplanner.transit.raptor.api.transit.TripScheduleInfo;
import org.opentripplanner.transit.raptor.util.PathStringBuilder;
import org.opentripplanner.transit.raptor.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * The result path of a Raptor search describing the one possible journey.
 *
 * @param <T> The TripSchedule type defined by the user of the range raptor API.
 */
public final class Path<T extends TripScheduleInfo> {
    private final int startTime;
    private final int endTime;
    private final int numberOfTransfers;
    private final int cost;
    private final AccessPathLeg<T> accessLeg;
    private EgressPathLeg<T> egressPathLeg = null;


    /**
     * Create a "dummy" path without legs. Can be used to test if a path is pareto optimal without
     * creating the hole path.
     */
    public static <T extends TripScheduleInfo> org.opentripplanner.transit.raptor.api.path.Path<T> dummyPath(
            int startTime, int endTime, int numberOfTransfers, int cost
    ) {
        return new org.opentripplanner.transit.raptor.api.path.Path<>(startTime, endTime, numberOfTransfers, cost);
    }

    /** @see #dummyPath(int, int, int, int) */
    private Path(int startTime, int endTime, int numberOfTransfers, int cost) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.numberOfTransfers = numberOfTransfers;
        this.cost = cost;
        this.accessLeg = null;
    }

    public Path(AccessPathLeg<T> accessLeg, int endTime, int numberOfTransfers, int cost) {
        this.accessLeg = accessLeg;
        this.startTime = accessLeg.fromTime();
        this.endTime = endTime;
        this.numberOfTransfers = numberOfTransfers;
        this.cost = cost;
    }

    /**
     * The journey start time. The departure time from the journey origin.
     */
    public final int startTime() {
        return startTime;
    }

    /**
     * The journey end time. The arrival time at the journey destination.
     */
    public final int endTime() {
        return endTime;
    }

    /**
     * The total journey duration in seconds.
     */
    public final int totalTravelDurationInSeconds() {
        return endTime - startTime;
    }

    /**
     * The total number of transfers for this journey.
     */
    public final int numberOfTransfers() {
        return numberOfTransfers;
    }

    /**
     * The total cost computed for this path. This is for debugging and filtering purposes.
     */
    public int cost() {
        return cost;
    }

    /**
     * The first leg of this journey - witch is linked to the next and so on.
     */
    public final AccessPathLeg<T> accessLeg() {
        return accessLeg;
    }

    /**
     * The last leg of this journey.
     */
    public final EgressPathLeg<T> egressLeg() {
        if(egressPathLeg == null) {
            PathLeg<T> leg = accessLeg;
            while (!leg.isEgressLeg()) leg = leg.nextLeg();
            egressPathLeg = leg.asEgressLeg();
        }
        return egressPathLeg;
    }

    /**
     * Utility method to list all visited stops.
     */
    public List<Integer> listStops() {
        List<Integer> stops = new ArrayList<>();
        PathLeg<?> leg = accessLeg.nextLeg();

        while (!leg.isEgressLeg()) {
            if (leg.isTransitLeg()) {
                stops.add(leg.asTransitLeg().fromStop());
            }
            if (leg.isTransferLeg()) {
                stops.add(leg.asTransferLeg().fromStop());
            }
            leg = leg.nextLeg();
        }
        stops.add(leg.asEgressLeg().fromStop());
        return stops;
    }

    @Override
    public String toString() {
        PathStringBuilder buf = new PathStringBuilder();
        PathLeg<T> leg = accessLeg.nextLeg();

        buf.walk(accessLeg.duration());

        while (!leg.isEgressLeg()) {
            buf.sep();
            if(leg.isTransitLeg()) {
                buf.stop(leg.asTransitLeg().fromStop()).sep().transit(leg.fromTime(), leg.toTime());
            }
            // Transfer
            else {
                buf.stop(leg.asTransferLeg().fromStop()).sep().walk(leg.duration());
            }
            leg = leg.nextLeg();
        }
        buf.sep().stop(leg.asEgressLeg().fromStop()).sep().walk(leg.duration());

        return buf.toString() +
                " (tot: " + TimeUtils.timeToStrCompact(endTime-startTime) +
                (cost <= 0 ? "" : ", cost: " + cost) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        org.opentripplanner.transit.raptor.api.path.Path<?> path = (org.opentripplanner.transit.raptor.api.path.Path<?>) o;
        return startTime == path.startTime &&
                endTime == path.endTime &&
                numberOfTransfers == path.numberOfTransfers &&
                Objects.equals(accessLeg, path.accessLeg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime, endTime, numberOfTransfers, accessLeg);
    }
}
