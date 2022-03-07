package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.view;

import java.util.function.ToIntFunction;
import javax.validation.constraints.NotNull;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.StopArrivalState;
import org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.Stops;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;


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
    private final Stops<T> stops;
    private final TransitCalculator<T> transitCalculator;
    private final ToIntFunction<RaptorTripPattern> boardSlackProvider;

    public StopsCursor(
            Stops<T> stops,
            TransitCalculator<T> transitCalculator,
            ToIntFunction<RaptorTripPattern> boardSlackProvider
    ) {
        this.stops = stops;
        this.transitCalculator = transitCalculator;
        this.boardSlackProvider = boardSlackProvider;
    }

    public boolean exist(int round, int stop) {
        return stops.exist(round, stop);
    }

    public boolean transitExist(int round, int stop) {
        return stops.get(round, stop).arrivedByTransit();
    }

    /**
     * Return a fictive Transfer arrival for the rejected transfer stop arrival.
     */
    public Access<T> rejectedAccess(int round, RaptorTransfer accessPath, int arrivalTime) {
        return new Access<>(round, arrivalTime, accessPath);
    }

    /**
     * Return a fictive Transfer arrival for the rejected transfer stop arrival.
     */
    public Transfer<T> rejectedTransfer(int round, int fromStop, RaptorTransfer transfer, int toStop, int arrivalTime) {
        StopArrivalState<T> arrival = StopArrivalState.create();
        arrival.transferToStop(fromStop, arrivalTime, transfer);
        return new Transfer<>(round, toStop, arrival, this);
    }

    /**
     * Return a fictive Transit arrival for the rejected transit stop arrival.
     */
    public Transit<T> rejectedTransit(int round, int alightStop, int alightTime, T trip, int boardStop, int boardTime) {
            StopArrivalState<T> arrival = StopArrivalState.create();
            arrival.arriveByTransit(alightTime, boardStop, boardTime, trip);
            return new Transit<>(round, alightStop, arrival, this);
    }

    /**
     * Set cursor to the transit state at round and stop. Throws
     * runtime exception if round is 0 or no state exist.
     *
     * @param round the round to use.
     * @param stop the stop index to use.
     * @return the current transit state, if found
     */
    public Transit<T> transit(int round, int stop) {
       StopArrivalState<T> arrival = stops.get(round, stop);
        return new Transit<>(round, stop, arrival, this);
    }

    /**
     * Return the stop-arrival for the given round, stop and method of arrival(stopReachedOnBoard).
     * The returned arrival can be access(including flex), transfer or transit.
     */
    public ArrivalView<T> stop(int round, int stop, boolean stopReachedOnBoard) {
        return stopReachedOnBoard
                ? onBoardAccessOrTransit(round, stop)
                : bestStopArrival(round, stop);
    }

    /**
     * Return the earliest arrival for the round and stop. This method should
     * only be used if the next leg is on-board - it is not allowed if the
     * next leg is a transfer or on-street egress. The returned arrival is
     * an access(including flex), transfer or transit. Consider using the
     * "more specific" methods  {@link #stop(int, int, Transit)} or
     * {@link #onBoardAccessOrTransit(int, int)}.
     */
    public ArrivalView<T> bestStopArrival(int round, int stop) {
        //TODO OTP2 - If a access arrive on-board, then we should also allow
        //          - a transfer to be accepted without dropping the access.
        //          - This code need to be fixed when this problem is fixed.
        var arrival = stops.get(round, stop);
        if(arrival.arrivedByAccess()) {
            return new Access<>(round, arrival.time(), arrival.accessPath());
        }
        return newTransitOrTransfer(round, stop, arrival);
    }

    /**
     * Return the on-board arrival for the round and stop. This method is
     * gives the correct arrival if the next leg is on-street. The returned
     * arrival is an access(including flex), transfer or transit.
     */
    public ArrivalView<T> onBoardAccessOrTransit(int round, int stop) {
        var arrival = stops.get(round, stop);
        if(
                arrival.arrivedByAccess() &&
                arrival.accessPath().stopReachedOnBoard()
        ) {
            return new Access<>(round, arrival.time(), arrival.accessPath());
        }
        return new Transit<>(round, stop, arrival, this);
    }

    /**
     * Set cursor to stop followed by the give transit leg - this allows access to be time-shifted
     * according to the next transit boarding/departure time.
     */
    public ArrivalView<T> stop(int round, int stop, @NotNull Transit<T> nextTransitLeg) {
        var arrival = stops.get(round, stop);

        if(arrival.arrivedByAccess()) {
            return newAccessView(round, arrival, nextTransitLeg);
        }
        else {
            return newTransitOrTransfer(round, stop, arrival);
        }
    }

    /**
     * A access stop arrival, time-shifted according to the first transit boarding/departure time
     * and the possible restrictions in the access.
     * <p>
     * If given transit is {@code null}, then use the iteration departure time without any
     * time-shifted departure. This is used for logging and debugging, not for returned paths.
     */
    private ArrivalView<T> newAccessView(
        int round,
        StopArrivalState<T> arrival,
        Transit<T> transit
    ) {
        int transitDepartureTime = transit.boardTime();
        int boardSlack = boardSlackProvider.applyAsInt(transit.trip().pattern());

        // Preferred time-shifted access departure
        int preferredDepartureTime = transitCalculator.minusDuration(transitDepartureTime,
            boardSlack + arrival.transferDuration()
        );

        return newAccessView(round, preferredDepartureTime, arrival);
    }

    /**
     * A access stop arrival, time-shifted according to the {@code preferredDepartureTime} and the
     * possible restrictions in the access.
     */
    private ArrivalView<T> newAccessView(
            int round,
            int preferredDepartureTime,
            StopArrivalState<T> arrival
    ) {
        // Get the real 'departureTime' honoring the time-shift restriction in the access
        int departureTime = transitCalculator.departureTime(arrival.accessPath(),
                preferredDepartureTime
        );
        int arrivalTime = transitCalculator.plusDuration(departureTime,
                arrival.accessPath().durationInSeconds()
        );
        return new Access<>(round, arrivalTime, arrival.accessPath());
    }

    /**
     * A transfer is only present if it has the earliest arrival time. If, not the transit
     * is the has the best arrival time and we return that.
     */
    private ArrivalView<T> newTransitOrTransfer(int round, int stop, StopArrivalState<T> arrival) {
        return arrival.arrivedByTransfer()
                ? new Transfer<>(round, stop, arrival, this)
                : new Transit<>(round, stop, arrival, this);
    }
}
