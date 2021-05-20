package org.opentripplanner.transit.raptor.api.view;


import javax.annotation.Nullable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.TransitArrival;
import org.opentripplanner.util.time.DurationUtils;
import org.opentripplanner.util.time.TimeUtils;

/**
 * The purpose of the stop-arrival-view is to provide a common interface for stop-arrivals for
 * different implementations. The view hide the internal Raptor specific models, like the standard
 * and multi-criteria implementation. The internal models can be optimized for speed and/or
 * memory consumption, while the view provide one interface for mapping back to the users domain.
 * <p/>
 * The view is used by the debugging functionality and mapping to raptor paths (Raptor API).
 * <p/>
 * The view objects are only created to construct paths to be returned as part of debugging. This is
 * done for just a fraction of all stop arrivals, so there is no need to optimize performance nor
 * memory consumption fo view objects, but the view is designed with the Flyweight design pattern
 * in mind.
 * <p/>
 * NB! The scope of a view is only guaranteed to be valid for the duration of the method call - e.g.
 * debug callback.
 * <p/>
 * There is different kind of arrivals:
 * <ul>
 *     <li>Access - The first stop arrival, arriving after the access path.</li>
 *     <li>Transit - Arrived by transit</li>
 *     <li>Transfer - Arrived by transfer</li>
 *     <li>Egress - Arrived at destination</li>
 * </ul>
 * Use the "arrivedByX" methods before accessing the {@link #accessPath()}, {@link #transitPath()},
 * {@link #transferPath()} and {@link #egressPath()}.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface ArrivalView<T extends RaptorTripSchedule> {

    /**
     * Stop index where the arrival takes place.
     *
     * @throws UnsupportedOperationException if arrived at destination.
     */
    int stop();

    /**
     * The Range Raptor ROUND this stop is reached. Note! the destination
     * is reached in the same round as the associated egress stop arrival.
     */
    int round();

    /**
     * The arrival time for when the stop is reached including alight-slack.
     */
    int arrivalTime();

    /**
     * The accumulated cost. 0 (zero) is returned if no cost exist.
     */
    default int cost() {
        return 0;
    }

    /**
     * The previous stop arrival state or {@code null} if first arrival (access stop arrival).
     */
    ArrivalView<T> previous();

    /**
     * If it exist, return the most resent transit arrival visited. For a transit-stop-arrival
     * this is it self, for a transfer-stop-arrival it is the previous stop-arrival.
     * <p>
     * For access- and egress-arrivals, including flex this method return {@code null}.
     * <p>
     * The method should be as light as possible, since it is used during routing.
     */
    @Nullable
    default TransitArrival<T> mostResentTransitArrival() {
        return null;
    }

    /* Access stop arrival */

    /**
     * First stop arrival, arrived by a given access path.
     */
    default boolean arrivedByAccess() {
        return false;
    }

    default AccessPathView accessPath() {
        throw new UnsupportedOperationException();
    }


    /* Transit */

    /** @return true if transit arrival, otherwise false. */
    default boolean arrivedByTransit() {
        return false;
    }

    default TransitPathView<T> transitPath() {
        throw new UnsupportedOperationException();
    }


    /* Transfer */

    /** @return true if transfer arrival, otherwise false. */
    default boolean arrivedByTransfer() {
        return false;
    }

    default TransferPathView transferPath() {
        throw new UnsupportedOperationException();
    }


    /* Egress */

    /** @return true if destination arrival, otherwise false. */
    default boolean arrivedAtDestination() {
        return false;
    }

    default EgressPathView egressPath() {
        throw new UnsupportedOperationException();
    }

    /** Use this to easy create a to String implementation. */
    default String asString() {
        if(arrivedByAccess()) {
            return String.format(
                "Access { stop: %d, duration: %s, arrival-time: %s $%d }",
                stop(), DurationUtils.durationToStr(accessPath().access().durationInSeconds()),
                TimeUtils.timeToStrCompact(arrivalTime()),
                cost()
            );
        }
        if(arrivedByTransit()) {
            return String.format(
                "Transit { round: %d, stop: %d, pattern: %s, arrival-time: %s $%d }",
                round(),
                stop(),
                transitPath().trip().pattern().debugInfo(),
                TimeUtils.timeToStrCompact(arrivalTime()),
                cost()
            );
        }
        if(arrivedByTransfer()) {
            return String.format(
                "Walk { round: %d, stop: %d, arrival-time: %s $%d }",
                round(),
                stop(),
                TimeUtils.timeToStrCompact(arrivalTime()),
                cost()
            );
        }
        if(arrivedAtDestination()) {
            return String.format(
                "Egress { round: %d, from-stop: %d, duration: %s, arrival-time: %s $%d }",
                round(),
                previous().stop(),
                DurationUtils.durationToStr(egressPath().egress().durationInSeconds()),
                TimeUtils.timeToStrCompact(arrivalTime()),
                cost()
            );
        }
        throw new IllegalStateException("Unknown type of stop-arrival: " + getClass());
    }
}
