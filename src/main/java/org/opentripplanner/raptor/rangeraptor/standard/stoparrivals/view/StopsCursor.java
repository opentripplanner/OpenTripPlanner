package org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.view;

import java.util.function.ToIntFunction;
import javax.annotation.Nonnull;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripPattern;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.StdStopArrivals;
import org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.StopArrivalState;
import org.opentripplanner.raptor.rangeraptor.transit.TransitCalculator;

/**
 * Used to create a view to the internal StdRangeRaptor model and to navigate between stop arrivals.
 * Since view objects are only used for path and debugging operations, the view can create temporary
 * objects for each StopArrival. These view objects are temporary objects and when the algorithm
 * progress they might get invalid - so do not keep references to these objects bejond the scope of
 * of a the callers method.
 * <p/>
 * The design was originally done to support the FLyweight design pattern.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class StopsCursor<T extends RaptorTripSchedule> {

  private final StdStopArrivals<T> arrivals;
  private final TransitCalculator<T> transitCalculator;
  private final ToIntFunction<RaptorTripPattern> boardSlackProvider;

  public StopsCursor(
    StdStopArrivals<T> arrivals,
    TransitCalculator<T> transitCalculator,
    ToIntFunction<RaptorTripPattern> boardSlackProvider
  ) {
    this.arrivals = arrivals;
    this.transitCalculator = transitCalculator;
    this.boardSlackProvider = boardSlackProvider;
  }

  public boolean reachedOnBoard(int round, int stop) {
    var a = arrivals.get(round, stop);
    return a != null && a.reachedOnBoard();
  }

  public boolean reachedOnStreet(int round, int stop) {
    var a = arrivals.get(round, stop);
    if (a == null) {
      return false;
    }
    return a.arrivedByAccessOnStreet() || a.arrivedByTransfer();
  }

  /** Return a fictive access stop arrival. */
  public Access<T> fictiveAccess(int round, RaptorAccessEgress accessPath, int arrivalTime) {
    return new Access<>(round, arrivalTime, accessPath);
  }

  /**
   * Return a fictive Transfer stop arrival view. The arrival does not exist in the state, but is
   * linked with the previous arrival which is a "real" arrival present in the state. This enables
   * path generation.
   */
  public Transfer<T> fictiveTransfer(
    int round,
    int fromStop,
    RaptorTransfer transfer,
    int toStop,
    int arrivalTime
  ) {
    StopArrivalState<T> arrival = StopArrivalState.create();
    arrival.transferToStop(fromStop, arrivalTime, transfer);
    return new Transfer<>(round, toStop, arrival, this);
  }

  /**
   * Return a fictive Transit stop arrival view. The arrival does not exist in the state, but is
   * linked with the previous arrival which is a "real" arrival present in the state. This enables
   * path generation.
   */
  public Transit<T> fictiveTransit(
    int round,
    int alightStop,
    int alightTime,
    T trip,
    int boardStop,
    int boardTime
  ) {
    StopArrivalState<T> arrival = StopArrivalState.create();
    arrival.arriveByTransit(alightTime, boardStop, boardTime, trip);
    return new Transit<>(round, alightStop, arrival, this);
  }

  /**
   * Return the stop-arrival for the given round, stop and given access. There is no check that the
   * access exist.
   */
  public ArrivalView<T> access(int round, int stop, RaptorAccessEgress access) {
    var arrival = arrivals.get(round, stop);
    int time = access.stopReachedOnBoard() ? arrival.onBoardArrivalTime() : arrival.time();
    return new Access<>(round, time, access);
  }

  /**
   * Return the stop-arrival for the given round, stop and method of arrival(stopReachedOnBoard).
   * The returned arrival can be access(including flex), transfer or transit.
   *
   * @param stopReachedOnBoard if {@code true} the arrival returned must arrive onboard a vehicle,
   *                           if {@code false} the BEST arrival is returned on-street or on-board.
   */
  public ArrivalView<T> stop(int round, int stop, boolean stopReachedOnBoard) {
    var arrival = arrivals.get(round, stop);

    // We check for on-street arrivals first, since on-street is only available if it is better
    // than on-board arrivals
    if (!stopReachedOnBoard) {
      if (arrival.arrivedByAccessOnStreet()) {
        return newAccessViewByExactArrivalTime(round, arrival.time(), arrival.accessPathOnStreet());
      } else if (arrival.arrivedByTransfer()) {
        return new Transfer<>(round, stop, arrival, this);
      }
    }
    // On on-board arrivals can always be used, we do not care what the *stopReachedOnBoard* is.
    if (arrival.arrivedByAccessOnBoard()) {
      return newAccessViewByExactArrivalTime(
        round,
        arrival.onBoardArrivalTime(),
        arrival.accessPathOnBoard()
      );
    } else if (arrival.arrivedByTransit()) {
      return new Transit<>(round, stop, arrival, this);
    }
    // Should never get here...
    throw new IllegalStateException("Unknown arrival: " + arrival);
  }

  /**
   * Set cursor to stop followed by the give transit leg - this allows access to be time-shifted
   * according to the next transit boarding/departure time.
   */
  public ArrivalView<T> stop(int round, int stop, @Nonnull Transit<T> nextTransitLeg) {
    var arrival = arrivals.get(round, stop);

    if (arrival.arrivedByAccessOnStreet()) {
      return newAccessView(round, arrival.accessPathOnStreet(), nextTransitLeg);
    } else if (arrival.arrivedByTransfer()) {
      return new Transfer<>(round, stop, arrival, this);
    } else if (arrival.arrivedByAccessOnBoard()) {
      return newAccessView(round, arrival.accessPathOnBoard(), nextTransitLeg);
    } else if (arrival.arrivedByTransit()) {
      return new Transit<>(round, stop, arrival, this);
    }
    // Should never get here...
    throw new IllegalStateException("Unknown arrival: " + arrival);
  }

  /**
   * A access stop arrival, time-shifted according to the first transit boarding/departure time and
   * the possible restrictions in the access.
   * <p>
   * If given transit is {@code null}, then use the iteration departure time without any
   * time-shifted departure. This is used for logging and debugging, not for returned paths.
   */
  private ArrivalView<T> newAccessView(
    int round,
    RaptorAccessEgress accessPath,
    Transit<T> transit
  ) {
    int transitDepartureTime = transit.boardTime();
    int boardSlack = boardSlackProvider.applyAsInt(transit.trip().pattern());

    // Preferred time-shifted access departure
    int preferredDepartureTime = transitCalculator.minusDuration(
      transitDepartureTime,
      boardSlack + accessPath.durationInSeconds()
    );

    return newAccessViewByPreferredDepartureTime(round, preferredDepartureTime, accessPath);
  }

  /**
   * An access stop arrival, time-shifted according to the {@code preferredDepartureTime} and the
   * possible restrictions in the access.
   */
  private ArrivalView<T> newAccessViewByPreferredDepartureTime(
    int round,
    int preferredDepartureTime,
    RaptorAccessEgress accessPath
  ) {
    // Get the real 'departureTime' honoring the time-shift restriction in the access
    int departureTime = transitCalculator.departureTime(accessPath, preferredDepartureTime);

    if (departureTime == RaptorConstants.TIME_NOT_SET) {
      throw new IllegalStateException("The departureTime is not found");
    }

    int arrivalTime = transitCalculator.plusDuration(departureTime, accessPath.durationInSeconds());
    return newAccessViewByExactArrivalTime(round, arrivalTime, accessPath);
  }

  /**
   * An access stop arrival, NOT time-shifted! The arrival-time is used "as is".
   */
  private ArrivalView<T> newAccessViewByExactArrivalTime(
    int round,
    int exactArrivalTimeTime,
    RaptorAccessEgress accessPath
  ) {
    return new Access<>(round, exactArrivalTimeTime, accessPath);
  }
}
