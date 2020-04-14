package org.opentripplanner.transit.raptor.api.path;

import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.util.TimeUtils;

/**
 * A leg in a path. The legs are linked together from the first leg {@link AccessPathLeg}, to the last leg
 * {@link EgressPathLeg}. There must be at least one {@link TransitPathLeg}. Transit legs can follow each
 * other or be connected by one {@link TransferPathLeg}.
 * <p/>
 * This interface contain utility methods to _cast_ a leg into the concrete sub-type:
 * <pre>
 * if(leg.isTransitLeg()) {
 *     trip = leg.asTransitLeg().trip();
 * }
 * </pre>
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface PathLeg<T extends RaptorTripSchedule> {

    /**
     * The time when the leg start/depart from the leg origin.
     */
    int fromTime();


    /**
     * The time when the leg end/arrive at the leg destination.
     */
    int toTime();

    /**
     * Number of seconds to travel this leg. This does not include wait time.
     */
    default int duration() {
        return toTime() - fromTime();
    }

    /**
     * @return {@code true} if transit leg, if not {@code false}.
     */
    default boolean isTransitLeg() {
        return false;
    }

    /**
     * Utility method performing a cast to {@link TransitPathLeg}, use with care:
     * <pre>
     * if(it.isTransitLeg()} {
     *     TransitPathLeg&lt;T&gt; transit = it.asTransitLeg();
     *     ...
     *
     * }
     * </pre>
     */
    default TransitPathLeg<T> asTransitLeg() {
        //noinspection unchecked
        return (TransitPathLeg<T>) this;
    }

    /**
     * @return {@code true} if transfer leg, if not {@code false}.
     */
    default boolean isTransferLeg() {
        return false;
    }

    /**
     * Utility method performing a cast to {@link TransferPathLeg}, use with care:
     * <pre>
     * if(it.isTransferLeg()} {
     *     TransferPathLeg&lt;T&gt; transfer = it.asTransferLeg();
     *     ...
     *
     * }
     * </pre>
     */
    default TransferPathLeg<T> asTransferLeg() {
        //noinspection unchecked
        return (TransferPathLeg<T>) this;
    }

    /**
     * @return {@code true} if egress leg, if not {@code false}.
     */
    default boolean isEgressLeg() {
        return false;
    }

    /**
     * Utility method performing a cast to {@link EgressPathLeg}, use with care:
     * <pre>
     * if(it.isEgressLeg()} {
     *     EgressPathLeg&lt;T&gt; egress = it.asEgressLeg();
     *     ...
     *
     * }
     * </pre>
     */
    default EgressPathLeg<T> asEgressLeg() {
        return (EgressPathLeg<T>) this;
    }

    /**
     * <ul>
     * <li>An access leg is always followed by a transit leg.
     * <li>A transit leg can be followed by a new transit leg, an transfer leg or an egress leg.
     * <li>A transfer leg can only be followed by a transit leg.
     * <li>An egress leg is always the last leg and this method will throw a {@link UnsupportedOperationException}.
     * Use the {@link #isEgressLeg()} to check if the last leg is reached.
     * </ul>
     *
     * @return Next leg in path.
     */
    PathLeg<T> nextLeg();


    default String asString(int toStop) {
        return asString() + " -> Stop " + toStop;
    }

    default String asString() {
        return TimeUtils.timeToStrShort(fromTime()) + "-" +
                TimeUtils.timeToStrShort(toTime()) +
                "(" + TimeUtils.durationToStr(duration()) + ")"
                ;
    }
}
