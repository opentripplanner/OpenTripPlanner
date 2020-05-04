package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.view;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.AccessStopArrivalState;
import org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.StopArrivalState;
import org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.Stops;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;

import java.util.function.ToIntFunction;


/**
 * Used to create a view to the internal StdRangeRaptor model and to navigate
 * between stop arrivals. Since view objects are only used for path and debugging
 * operations, the view can create temporary objects for each StopArrival. These
 * view objects are temporary objects and when the algorithm progress they might
 * get invalid - so do not keep references to these objects bejond the scope of
 * of a the callers method.
 * <p/>
 * The design was originally done to support the FLyweight design pattern.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class StopsCursor<T extends RaptorTripSchedule> {
    private Stops<T> stops;
    private final TransitCalculator transitCalculator;
    private final ToIntFunction<RaptorTripPattern> boardSlackProvider;

    public StopsCursor(Stops<T> stops, TransitCalculator transitCalculator, ToIntFunction<RaptorTripPattern> boardSlackProvider) {
        this.stops = stops;
        this.transitCalculator = transitCalculator;
        this.boardSlackProvider = boardSlackProvider;
    }

    public boolean exist(int round, int stop) {
        return stops.exist(round, stop);
    }


    /**
     * Return a fictive Transit arrival for the rejected transit stop arrival.
     */
    public StopArrivalViewAdapter.Transit<T> rejectedTransit(int round, int alightStop, int alightTime, T trip, int boardStop, int boardTime) {
            StopArrivalState<T> arrival = new StopArrivalState<>();
            arrival.arriveByTransit(alightTime, boardStop, boardTime, trip);
            return new StopArrivalViewAdapter.Transit<>(round, alightStop, arrival, this);
    }

    /**
     * Return a fictive Transfer arrival for the rejected transfer stop arrival.
     */
    public StopArrivalViewAdapter.Transfer<T> rejectedTransfer(int round, int fromStop, RaptorTransfer transferLeg, int toStop, int arrivalTime) {
            StopArrivalState<T> arrival = new StopArrivalState<>();
            arrival.transferToStop(fromStop, arrivalTime, transferLeg.durationInSeconds());
            return new StopArrivalViewAdapter.Transfer<>(round, toStop, arrival, this);
    }

    /**
     * A access stop arrival, time-shifted according to the first transit boarding/departure time
     */
    ArrivalView<T> access(int stop, StopArrivalViewAdapter.Transit<T> transitLeg) {
        return newAccessView(
                stop,
                transitLeg.departureTime(),
                boardSlackProvider.applyAsInt(transitLeg.trip().pattern())
        );
    }

    /**
     * Set cursor to the transit state at round and stop. Throws
     * runtime exception if round is 0 or no state exist.
     *
     * @param round the round to use.
     * @param stop the stop index to use.
     * @return the current transit state, if found
     */
    public StopArrivalViewAdapter.Transit<T> transit(int round, int stop) {
        StopArrivalState<T> state = stops.get(round, stop);
        return new StopArrivalViewAdapter.Transit<>(round, stop, state, this);
    }

    public ArrivalView<T> stop(int round, int stop) {
        return round == 0 ? newAccessView(stop) : newTransitOrTransferView(round, stop);
    }

    /**
     * Access without known transit, uses the iteration departure time without time shift
     */
    private ArrivalView<T> newAccessView(int stop) {
        AccessStopArrivalState<T> arrival = stops.get(0, stop).asAccessStopArrivalState();
        int departureTime = transitCalculator.minusDuration(arrival.time(), arrival.accessDuration());
        return new StopArrivalViewAdapter.Access<>(stop, departureTime, arrival.time(), arrival.access());
    }

    /**
     * A access stop arrival, time-shifted according to the first transit boarding/departure time.
     */
    private ArrivalView<T> newAccessView(int stop, int transitDepartureTime, int boardSlack) {
        AccessStopArrivalState<T> state = stops.get(0, stop).asAccessStopArrivalState();
        int departureTime = transitCalculator.minusDuration(transitDepartureTime, state.accessDuration() + boardSlack);
        int arrivalTime = transitCalculator.plusDuration(departureTime, state.accessDuration());
        return new StopArrivalViewAdapter.Access<>(stop, departureTime, arrivalTime, state.access());
    }

    private ArrivalView<T> newTransitOrTransferView(int round, int stop) {
        StopArrivalState<T> state = stops.get(round, stop);

        return state.arrivedByTransfer()
                ? new StopArrivalViewAdapter.Transfer<>(round, stop, state, this)
                : new StopArrivalViewAdapter.Transit<>(round, stop, state, this);
    }

    int departureTime(int arrivalTime, int legDuration) {
        return transitCalculator.minusDuration(arrivalTime, legDuration);
    }
}
