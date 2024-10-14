package org.opentripplanner.raptor.api.view;

import java.util.function.IntFunction;
import javax.annotation.Nullable;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.raptor.api.model.PathLegType;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.RaptorValueFormatter;
import org.opentripplanner.raptor.api.model.TransitArrival;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.DefaultCostCalculator;

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
   * The Range Raptor ROUND this stop is reached. Note! the destination is reached in the same round
   * as the associated egress stop arrival.
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

  /* Access stop arrival */

  /**
   * First stop arrival, arrived by a given access path.
   */
  PathLegType arrivedBy();

  default boolean arrivedBy(PathLegType expected) {
    return arrivedBy().is(expected);
  }

  default AccessPathView accessPath() {
    throw new UnsupportedOperationException();
  }

  /* Transit */

  default TransitPathView<T> transitPath() {
    throw new UnsupportedOperationException();
  }

  /* Transfer */

  default RaptorTransfer transfer() {
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

  boolean arrivedOnBoard();

  /** Use this to create a {@code toString()} implementation. */
  default String asString() {
    String arrival =
      "[" +
      TimeUtils.timeToStrCompact(arrivalTime()) +
      cost(c1(), DefaultCostCalculator.ZERO_COST, RaptorValueFormatter::formatC1) +
      cost(c2(), RaptorConstants.NOT_SET, RaptorValueFormatter::formatC2) +
      "]";
    return switch (arrivedBy()) {
      case ACCESS -> String.format(
        "Access { stop: %d, arrival: %s, path: %s }",
        stop(),
        arrival,
        accessPath().access()
      );
      case TRANSIT -> String.format(
        "Transit { round: %d, stop: %d, arrival: %s, pattern: %s }",
        round(),
        stop(),
        arrival,
        transitPath().trip().pattern().debugInfo()
      );
      case TRANSFER -> String.format(
        "Walk { round: %d, stop: %d, arrival: %s, path: %s }",
        round(),
        stop(),
        arrival,
        transfer()
      );
      case EGRESS -> String.format(
        "Egress { round: %d, from-stop: %d, arrival: %s, path: %s }",
        round(),
        egressPath().egress().stop(),
        arrival,
        egressPath().egress()
      );
    };
  }

  private static String cost(int cost, int defaultValue, IntFunction<String> toString) {
    return cost == defaultValue ? "" : " " + toString.apply(cost);
  }
}
