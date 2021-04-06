package org.opentripplanner.transit.raptor.api.path;

import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.util.PathStringBuilder;
import org.opentripplanner.util.time.DurationUtils;
import org.opentripplanner.util.time.TimeUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * The result path of a Raptor search describing the one possible journey.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class Path<T extends RaptorTripSchedule> implements Comparable<Path<T>>{
    private final int iterationDepartureTime;
    private final int startTime;
    private final int endTime;
    private final int numberOfTransfers;
    private final int generalizedCost;
    private final AccessPathLeg<T> accessLeg;
    private final EgressPathLeg<T> egressPathLeg;

    /** @see #dummyPath(int, int, int, int, int) */
    private Path(
            int iterationDepartureTime,
            int startTime,
            int endTime,
            int numberOfTransfers,
            int generalizedCost
    ) {
        this.iterationDepartureTime = iterationDepartureTime;
        this.startTime = startTime;
        this.endTime = endTime;
        this.numberOfTransfers = numberOfTransfers;
        this.generalizedCost = generalizedCost;
        this.accessLeg = null;
        this.egressPathLeg = null;
    }

    public Path(int iterationDepartureTime, AccessPathLeg<T> accessLeg, int generalizedCost) {
        this.iterationDepartureTime = iterationDepartureTime;
        this.startTime = accessLeg.fromTime();
        this.generalizedCost = generalizedCost;
        this.accessLeg = accessLeg;
        this.egressPathLeg = findEgressLeg(accessLeg);
        this.numberOfTransfers = countNumberOfTransfers(accessLeg, egressPathLeg);
        this.endTime = egressPathLeg.toTime();
    }

    /**
     * Create a "dummy" path without legs. Can be used to test if a path is pareto optimal without
     * creating the hole path.
     */
    public static <T extends RaptorTripSchedule> Path<T> dummyPath(
        int iteration, int startTime, int endTime, int numberOfTransfers, int cost
    ) {
        return new Path<>(iteration, startTime, endTime, numberOfTransfers, cost);
    }

    /**
     * The Range Raptor iteration departure time. This can be used in the path-pareto-function to make sure
     * all results found in previous iterations are kept, and not dominated by new results.
     * This is used for the time-table view.
     */
    public final int rangeRaptorIterationDepartureTime() {
        return iterationDepartureTime;
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
    public final int travelDurationInSeconds() {
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
    public int generalizedCost() {
        return generalizedCost;
    }

    /**
     * The first leg/path of this journey - witch is linked to the next and so on. The leg
     * can contain sub-legs, for example: walk-flex-walk.
     *
     */
    public final AccessPathLeg<T> accessLeg() {
        return accessLeg;
    }

    /**
     * The last leg of this journey. The leg can contain sub-legs, for example: walk-flex-walk.
     */
    public final EgressPathLeg<T> egressLeg() {
        return egressPathLeg;
    }

    /**
     * Utility method to list all visited stops.
     */
    public List<Integer> listStops() {
        return accessLeg.nextLeg().stream()
            .map(PathLeg::fromStop)
            .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        PathStringBuilder buf = new PathStringBuilder();
        if(accessLeg != null) {
            buf.accessEgress(accessLeg.access());
            for (PathLeg<T> leg : accessLeg.nextLeg().iterator()) {
                buf.sep();
                if (leg.isTransitLeg()) {
                    TransitPathLeg<T> transitLeg = leg.asTransitLeg();
                    buf.stop(transitLeg.fromStop()).sep().transit(
                        transitLeg.trip().pattern().debugInfo(),
                        transitLeg.fromTime(),
                        transitLeg.toTime()
                    );
                }
                else if (leg.isTransferLeg()) {
                    buf.stop(leg.asTransferLeg().fromStop()).sep().walk(leg.duration());
                }
                // Egress
                else {
                    buf.stop(leg.fromStop()).sep().accessEgress(leg.asEgressLeg().egress());
                }
            }
        }
        return buf.toString() +
                " [" + TimeUtils.timeToStrLong(startTime) +
                " " + TimeUtils.timeToStrLong(endTime) +
                " " + DurationUtils.durationToStr(endTime - startTime) +
                (generalizedCost == 0 ? "" : ", cost: " + generalizedCost) + "]";
    }

    /** Return the duration of time spent onBoard - excluding slack. */
    public int transitDuration() {
        return legStream()
            .filter(PathLeg::isTransitLeg)
            .mapToInt(PathLeg::duration)
            .sum();
    }

    public Stream<PathLeg<T>> legStream() {
        return accessLeg.stream();
    }

    public Iterable<PathLeg<T>> legIterable() {
        return accessLeg.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        Path<?> path = (Path<?>) o;
        return startTime == path.startTime &&
                endTime == path.endTime &&
                numberOfTransfers == path.numberOfTransfers &&
                Objects.equals(accessLeg, path.accessLeg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime, endTime, numberOfTransfers, accessLeg);
    }

    /**
     * Sort paths in order:
     * <ol>
     *   <li>Earliest arrival time first,
     *   <li>Then latest departure time
     *   <li>Then lowest cost
     *   <li>Then lowest number of transfers
     * </ol>
     */
    @Override
    public int compareTo(Path<T> other) {
        int c = endTime - other.endTime;
        if(c != 0) { return c; }
        c = other.startTime - startTime;
        if(c != 0) { return c; }
        c = generalizedCost - other.generalizedCost;
        if(c != 0) { return c; }
        c = numberOfTransfers - other.numberOfTransfers;
        return c;
    }


    /* private methods */

    private static <S extends RaptorTripSchedule> EgressPathLeg<S> findEgressLeg(PathLeg<S> leg) {
        return (EgressPathLeg<S>) leg.stream().reduce((a,b) -> b).orElseThrow();
    }

    private static <S extends RaptorTripSchedule> int countNumberOfTransfers(
        AccessPathLeg<S> accessLeg, EgressPathLeg<S> egressPathLeg
    ) {

        return accessLeg.access().numberOfRides()
            // Skip first transit
            + (int)accessLeg.nextLeg().nextLeg().stream().filter(PathLeg::isTransitLeg).count()
            + egressPathLeg.egress().numberOfRides();
    }
}
