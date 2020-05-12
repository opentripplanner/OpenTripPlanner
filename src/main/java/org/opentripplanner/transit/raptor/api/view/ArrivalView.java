package org.opentripplanner.transit.raptor.api.view;


import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.util.TimeUtils;

/**
 * The purpose of the stop arrival view is to provide a common interface for
 * stop arrivals. The view hide the internal Raptor specific models. The internal
 * models can be optimized for  speed and/or memory consumption, while the view
 * provide one interface for mapping back to the users domain.
 * <p/>
 * The view is used by the debugging functionality and mapping to paths.
 * <p/>
 * The view objects are only created to construct paths to be returned as part
 * of debugging. This is just a fraction of all stop arrivals, so there is no need
 * to optimize performance nor memory consumption fo view objects, but the view
 * is designed with the Flyweight design pattern in mind.
 * <p/>
 * NB! The scope of a view is only guaranteed to be valid for the duration of the
 * method call - e.g. debug callback.
 * <p/>
 * There is different kind of arrivals:
 * <ul>
 *     <li>Access - The first stop arrival, arriving after the access leg.</li>
 *     <li>Transit - Arrived by transit</li>
 *     <li>Transfer - Arrived by transfer</li>
 *     <li>Egress - Arrived at destination</li>
 * </ul>
 * Use the "arrivedByX" methods before calling arrival type specific method. For example the {@link #trip()} method throws an exception if invoked on a Egress
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
     * The access or egress connecting this leg to the start or end location of the search.
     *
     * @throws UnsupportedOperationException if did not arrive via street network.
     */
    RaptorTransfer accessEgress();

    /**
     * The Range Raptor ROUND this stop is reached. Note! the destination
     * is reached in the same round as the associated egress stop arrival.
     */
    int round();

    /**
     * The last leg departure time.
     */
    int departureTime();

    /**
     * The arrival time for when the stop is reached
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

    /* Access stop arrival */

    /**
     * First stop arrival, arrived by a given access leg.
     */
    default boolean arrivedByAccessLeg() {
        return false;
    }

    /* Transit */

    /** @return true if transit arrival, otherwise false. */
    default boolean arrivedByTransit() {
        return false;
    }

    /** @throws UnsupportedOperationException if not arrived by transit.  */
    default int boardStop() {
        throw new UnsupportedOperationException();
    }

    /** @throws UnsupportedOperationException if not arrived by transit.  */
    default T trip() {
        throw new UnsupportedOperationException();
    }

    /* Transfer */

    /** @return true if transfer arrival, otherwise false. */
    default boolean arrivedByTransfer() {
        return false;
    }

    /** @throws UnsupportedOperationException if not arrived by transfer or arrived at destination()}.  */
    default int transferFromStop() {
        throw new UnsupportedOperationException();
    }

    /* Egress */

    /** @return true if destination arrival, otherwise false. */
    default boolean arrivedAtDestination() {
        return false;
    }

    /**
     * Describe type of leg/mode. This is used for logging/debugging.
     */
    default String legType() {
        if (arrivedByAccessLeg()) {
            return "Access";
        }
        if (arrivedByTransit()) {
            return "Transit";
        }
        // We use Walk instead of Transfer so it is easier to distinguish from Transit
        if (arrivedByTransfer()) {
            return  "Walk";
        }
        if (arrivedAtDestination()) {
            return  "Egress";
        }
        throw new IllegalStateException("Unknown mode for: " + this);
    }

    /** Use this to easy create a to String implementation. */
    default String asString() {
        if(arrivedAtDestination()) {
            return String.format(
                    "%s { From stop: %d, Time: %s (%s), Cost: %d }",
                    getClass().getSimpleName(),
                    transferFromStop(),
                    TimeUtils.timeToStrCompact(arrivalTime()),
                    TimeUtils.timeToStrCompact(departureTime()),
                    cost()
            );
        }
        return String.format(
                "%s { Rnd: %d, Stop: %d, Time: %s (%s), Cost: %d }",
                getClass().getSimpleName(),
                round(),
                stop(),
                TimeUtils.timeToStrCompact(arrivalTime()),
                TimeUtils.timeToStrCompact(departureTime()),
                cost()
        );
    }
}
