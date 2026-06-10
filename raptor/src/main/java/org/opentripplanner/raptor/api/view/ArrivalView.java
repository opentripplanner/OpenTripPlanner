package org.opentripplanner.raptor.api.view;

import static org.opentripplanner.raptor.api.model.RaptorValueType.C1;
import static org.opentripplanner.raptor.api.model.RaptorValueType.C2;
import static org.opentripplanner.raptor.api.model.RaptorValueType.ROUNDS;

import java.util.function.IntFunction;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * The purpose of the stop-arrival-view is to provide a common interface for stop-arrivals for
 * different implementations. The view hide the internal Raptor specific models, like the standard
 * and multi-criteria implementation. The internal models can be optimized for speed and/or memory
 * consumption, while the view provide one interface for mapping back to the users domain.
 * <p/>
 * The view is used by the debugging functionality and mapping to raptor paths (Raptor API).
 * <p/>
 * The view objects are only created to construct paths to be returned as part of debugging. This is
 * done for just a fraction of all stop arrivals, so there is no need to optimize performance nor
 * memory consumption fo view objects, but the view is designed with the Flyweight design pattern in
 * mind.
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
 * {@link #transfer()} and {@link #egressPath()}.
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
   * The RangeRaptor round. Transit arrivals increment the round by one; transfer arrivals
   * stay in the same round as the transit they follow. Dominance of transits over transfers at
   * via/access/egress stops is handled by event listeners, not by inflating the round counter.
   * This gives better performance.
   */
  int round();

  /**
   * Return number of transfers used to reach the stop.
   */
  default int numberOfTransfers() {
    return round() - 1;
  }

  /**
   * {@code true} if this arrival represents a simple access arrival without any embedded rides.
   * FLEX access should not be added in round 0 (the first round).
   * <p>
   * This method is used to add special functionality for the first transit leg and the next leg.
   * For example adding transfer cost to all boardings except the fist one.
   */
  default boolean isFirstRound() {
    return round() == 0;
  }

  /**
   * The arrival time for when the stop is reached including alight-slack.
   */
  int arrivalTime();

  /**
   * The accumulated criteria ONE(usually used to store the generalized-cost, but is not
   * limited to this). {@link RaptorCostCalculator#ZERO_COST} is returned if no cost exist.
   */
  int c1();

  /**
   * The accumulated criteria TWO. Can be used for any int criteria used during routing. A
   * state with c1 and c2 is created dynamically if c2 is in use, if not this method will
   * throw an exception.
   * <p>
   * {@link RaptorConstants#NOT_SET} is returned if no criteria exist, but the model
   * support it.
   */
  int c2();

  /**
   * The previous stop arrival state or {@code null} if first arrival (access stop arrival).
   */
  @Nullable
  ArrivalView<T> previous();

  /**
   * If it exists, return the most recent transit arrival visited. For a transit-stop-arrival this
   * is itself, for a transfer-stop-arrival it is the previous stop-arrival.
   * <p>
   * For access- and egress-arrivals, including flex this method return {@code null}.
   * <p>
   * The method should be as light as possible, since it is used during routing.
   */
  @Nullable
  default TransitArrival<T> mostRecentTransitArrival() {
    return null;
  }

  /**
   * The type of leg used to arrive at this stop.
   */
  PathLegType arrivedBy();

  /**
   * Return {@code true} if this arrival was reached by the given {@code expected} leg type.
   */
  default boolean arrivedBy(PathLegType expected) {
    return arrivedBy().is(expected);
  }

  /**
   * The access path view for this arrival. Only valid when {@link #arrivedBy()} returns
   * {@link PathLegType#ACCESS}.
   */
  default AccessPathView accessPath() {
    throw new UnsupportedOperationException();
  }

  /**
   * The transit path view for this arrival. Only valid when {@link #arrivedBy()} returns
   * {@link PathLegType#TRANSIT}.
   */
  default TransitPathView<T> transitPath() {
    throw new UnsupportedOperationException();
  }

  /**
   * The transfer used to reach this stop. Only valid when {@link #arrivedBy()} returns
   * {@link PathLegType#TRANSFER}.
   */
  default RaptorTransfer transfer() {
    throw new UnsupportedOperationException();
  }

  /**
   * The egress path view for this arrival. Only valid when {@link #arrivedBy()} returns
   * {@link PathLegType#EGRESS}.
   */
  default EgressPathView egressPath() {
    throw new UnsupportedOperationException();
  }

  /**
   * Return {@code true} if the traveller arrived at this stop while on board a vehicle (i.e. via
   * transit or flex with an in-seat transfer), as opposed to arriving on foot via access or
   * transfer.
   */
  boolean arrivedOnBoard();

  /**
   * Use this to create a {@code toString()} implementation.
   */
  default String asString() {
    String vector =
      TimeUtils.timeToStrCompact(arrivalTime()) +
      " " +
      ROUNDS.format(round()) +
      cost(c1(), RaptorCostCalculator.ZERO_COST, C1::format) +
      cost(c2(), RaptorConstants.NOT_SET, C2::format);

    return switch (arrivedBy()) {
      case ACCESS -> String.format("Access [%s] (%s)", vector, accessPath().access());
      case TRANSIT -> String.format(
        "Transit [%s] (%s ~ %s)",
        vector,
        transitPath().trip().pattern().debugInfo(),
        stop()
      );
      case TRANSFER -> String.format("Transfer [%s] (%s)", vector, transfer());
      case EGRESS -> String.format("Egress [%s] (%s)", vector, egressPath().egress());
    };
  }

  private static String cost(int cost, int defaultValue, IntFunction<String> toString) {
    return cost == defaultValue ? "" : " " + toString.apply(cost);
  }
}
