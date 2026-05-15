package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals;

import static org.opentripplanner.raptor.api.view.PathLegType.TRANSIT;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import java.util.BitSet;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorTripScheduleStopPosition;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.raptor.rangeraptor.internalapi.OnTripAccessArrivals;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop.ArrivalParetoSetComparatorFactory;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop.McStopArrival;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.util.BitSetIterator;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;
import org.opentripplanner.raptor.util.paretoset.ParetoSet;
import org.opentripplanner.raptor.util.paretoset.ParetoSetEventListener;

/**
 * Holds the multi-criteria stop arrival state for all stops. Each stop has its own
 * {@link ParetoSet} of {@link McStopArrival}s, keeping only Pareto-optimal arrivals across
 * arrival time, round, and cost. Stops that need to distinguish on-board arrivals (e.g. for
 * via-connection pass-through) use an extended comparator.
 * <p>
 * Also tracks which stops have been touched since the last call to
 * {@link #clearTouchedStopsAndSetStopMarkers()}, so the routing loop can iterate only over
 * relevant stops.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class McStopArrivals<T extends RaptorTripSchedule> {

  private final ParetoSet<McStopArrival<T>>[] arrivals;
  private final TIntObjectMap<OnTripAccessArrivals<T>> onBoardTripArrivalsByRouteQueue;

  private final BitSet touchedStops;

  private final DebugHandlerFactory<T> debugHandlerFactory;
  private final DebugStopArrivalsStatistics debugStats;
  private final ParetoComparator<McStopArrival<T>> comparator;

  /**
   * @param nStops             total number of stops in the transit network.
   * @param onBoardArrivalStops stops where on-board arrivals is better than on-street arrivals;
   *                           these stops need a pareto comparator that also considers whether the
   *                           traveller arrived on board.
   * @param arrivalListeners   per-stop listeners (via, egress, debug), keyed by stop index.
   * @param comparatorFactory  factory for creating the Pareto comparators used per stop.
   * @param debugHandlerFactory factory for debug handlers and loggers.
   */
  public McStopArrivals(
    int nStops,
    TIntSet onBoardArrivalStops,
    TIntObjectMap<ParetoSetEventListener<ArrivalView<T>>> arrivalListeners,
    ArrivalParetoSetComparatorFactory<McStopArrival<T>> comparatorFactory,
    DebugHandlerFactory<T> debugHandlerFactory
  ) {
    //noinspection unchecked
    this.arrivals = (ParetoSet<McStopArrival<T>>[]) new ParetoSet[nStops];
    this.onBoardTripArrivalsByRouteQueue = new TIntObjectHashMap<>();
    this.touchedStops = new BitSet(nStops);
    this.comparator = comparatorFactory.compareArrivalTimeRoundAndCost();
    this.debugHandlerFactory = debugHandlerFactory;
    this.debugStats = new DebugStopArrivalsStatistics(debugHandlerFactory.debugLogger());

    // Comparator for stops that have arrival listeners with a on-board trip-to-trip via
    // connection.
    var onBoardComparator = comparatorFactory.compareArrivalTimeRoundCostAndOnBoardArrival();
    for (int stop : arrivalListeners.keys()) {
      var comp = onBoardArrivalStops.contains(stop) ? onBoardComparator : comparator;
      this.arrivals[stop] = ParetoSet.of(comp, arrivalListeners.get(stop));
    }

    for (var it = onBoardArrivalStops.iterator(); it.hasNext(); ) {
      int stop = it.next();
      if (this.arrivals[stop] == null) {
        this.arrivals[stop] = ParetoSet.of(
          onBoardComparator,
          debugHandlerFactory.paretoSetStopArrivalListener(stop)
        );
      }
    }
  }

  /** Return {@code true} if the given stop has at least one Pareto-optimal arrival. */
  public boolean reached(int stopIndex) {
    return arrivals[stopIndex] != null && !arrivals[stopIndex].isEmpty();
  }

  /**
   * Return the best (earliest) arrival time across all arrivals at the given stop.
   * <p>
   * Slow — do not use during routing.
   */
  public int bestArrivalTime(int stopIndex) {
    return minInt(arrivals[stopIndex].stream(), McStopArrival::arrivalTime);
  }

  /**
   * Return {@code true} if the stop was reached by at least one transit leg.
   * <p>
   * Slow — do not use during routing.
   */
  public boolean reachedByTransit(int stopIndex) {
    return (
      arrivals[stopIndex] != null &&
      arrivals[stopIndex].stream().anyMatch(a -> a.arrivedBy(TRANSIT))
    );
  }

  /**
   * Return the best (earliest) transit arrival time at the given stop.
   * <p>
   * Slow — do not use during routing.
   */
  public int bestTransitArrivalTime(int stopIndex) {
    return transitStopArrivalsMinInt(stopIndex, McStopArrival::arrivalTime);
  }

  /**
   * Return the smallest number of transfers among all transit arrivals at the given stop.
   * <p>
   * Slow — do not use during routing.
   */
  public int smallestNumberOfTransfers(int stopIndex) {
    return transitStopArrivalsMinInt(stopIndex, McStopArrival::numberOfTransfers);
  }

  /** Return {@code true} if any stop has been touched since the last marker reset. */
  public boolean updateExist() {
    return !touchedStops.isEmpty();
  }

  /** Return an iterator over all stops touched since the last marker reset. */
  public IntIterator stopsTouchedIterator() {
    return new BitSetIterator(touchedStops);
  }

  /**
   * Add a stop arrival to the Pareto set for its stop. If the arrival is accepted,
   * the stop is marked as touched.
   */
  public void addStopArrival(McStopArrival<T> arrival) {
    boolean added = findOrCreateSet(arrival.stop()).add(arrival);

    if (added) {
      touchedStops.set(arrival.stop());
    }
  }

  public void debugStateInfo() {
    debugStats.debugStatInfo(arrivals);
  }

  /**
   * Return {@code true} if the given stop has any arrivals added after the last marker was set
   * (i.e. arrivals in the current round).
   */
  public boolean hasArrivalsAfterMarker(int stop) {
    var it = arrivals[stop];
    return it != null && it.hasElementsAfterMarker();
  }

  /**
   * Return all arrivals at the given stop that were added after the last marker was set.
   * <p>
   * The semantics depend on when this is called in the round lifecycle:
   * <ul>
   *   <li>Called from the <b>boarding step</b>: returns transit and transfer arrivals from the
   *       previous round (the marker was advanced past them at the end of that round's transit
   *       scan, but the arrivals remain readable).</li>
   *   <li>Called from the <b>transfer step</b>: returns only the transit alights from the current
   *       round (the marker was just advanced past the previous round's arrivals and the new
   *       transit alights were committed).</li>
   * </ul>
   */
  public Iterable<McStopArrival<T>> listArrivalsAfterMarker(final int stop) {
    var it = arrivals[stop];
    // Avoid creating new objects in a tight loop
    return it == null ? Collections::emptyIterator : it.elementsAfterMarker();
  }

  /**
   * For each touched stop, advance the marker to the end of its Pareto set, then clear the
   * touched-stop tracking.
   * <p>
   * Called at two points in the round lifecycle (see
   * {@link McRangeRaptorWorkerState} for the full lifecycle description):
   * <ul>
   *   <li><b>Setup iteration</b>: advances the marker past all arrivals, including transfer
   *       arrivals left over from the previous iteration's last round, so they are not
   *       re-explored in the new iteration.</li>
   *   <li><b>Transits for round complete</b>: advances the marker past the previous round's
   *       arrivals before committing the current round's transit alights. This makes the new
   *       transit alights visible to the transfer step while hiding the older arrivals.</li>
   * </ul>
   */
  public void clearTouchedStopsAndSetStopMarkers() {
    IntIterator it = stopsTouchedIterator();
    while (it.hasNext()) {
      arrivals[it.next()].markAtEndOfSet();
    }
    touchedStops.clear();
  }

  /**
   * Remove and return the queued on-board trip arrivals for the given route, or {@code null} if
   * none exist. Each call consumes the arrivals — they are removed from the queue.
   */
  @Nullable
  public OnTripAccessArrivals<T> consumeOnTripStopArrivalsForRoute(int routeIndex) {
    return onBoardTripArrivalsByRouteQueue.remove(routeIndex);
  }

  /**
   * Queue an on-board trip arrival for processing when the corresponding route is visited.
   * The arrival is grouped by route index so that the routing loop can retrieve all on-board
   * arrivals for a route in one call to {@link #consumeOnTripStopArrivalsForRoute}.
   * <p>
   * Also ensures the Pareto set for {@code applyToStopIndex} is initialised and marks the stop
   * as touched so the routing loop will visit it.
   *
   * @param boardingArrival            the arrival state from which boarding occurs.
   * @param startRoutingAtStopIndex    the stop index where the new ride is applied in the
   *                                   algorithm. The next stop in pattern will be the first
   *                                   possible stop to alight. The boarding might be at an earlier
   *                                   stop.
   * @param startRoutingAtStopPosition Same as above, but the stop position in the trip pattern.
   * @param boardingConstraint         the trip and stop-position for where the boarding happens.
   */
  public void addOnBoardTripArrival(
    ArrivalView<T> boardingArrival,
    int startRoutingAtStopIndex,
    int startRoutingAtStopPosition,
    RaptorTripScheduleStopPosition boardingConstraint
  ) {
    int routeIndex = boardingConstraint.routeIndex();
    var arrivalsForRoute = onBoardTripArrivalsByRouteQueue.get(routeIndex);

    if (arrivalsForRoute == null) {
      arrivalsForRoute = new OnTripAccessArrivals<>();
      onBoardTripArrivalsByRouteQueue.put(routeIndex, arrivalsForRoute);
    }
    arrivalsForRoute.add(boardingArrival, startRoutingAtStopPosition, boardingConstraint);

    // Then update the state, both touchedStops and init the pareto-set for the given stop to
    // prevent NPE when the state is fetched later. The set is empty, which is ok.
    findOrCreateSet(startRoutingAtStopIndex);
    touchedStops.set(startRoutingAtStopIndex);
  }

  /* private methods */

  private ParetoSet<McStopArrival<T>> findOrCreateSet(final int stop) {
    if (arrivals[stop] == null) {
      arrivals[stop] = ParetoSet.of(
        comparator,
        debugHandlerFactory.paretoSetStopArrivalListener(stop)
      );
    }
    return arrivals[stop];
  }

  private int transitStopArrivalsMinInt(int stopIndex, Function<McStopArrival<T>, Integer> mapper) {
    var transitArrivals = arrivals[stopIndex].stream().filter(a -> a.arrivedBy(TRANSIT));
    return minInt(transitArrivals, mapper);
  }

  private int minInt(
    Stream<McStopArrival<T>> transitArrivals,
    Function<McStopArrival<T>, Integer> mapper
  ) {
    return transitArrivals.mapToInt(mapper::apply).min().orElseThrow();
  }
}
