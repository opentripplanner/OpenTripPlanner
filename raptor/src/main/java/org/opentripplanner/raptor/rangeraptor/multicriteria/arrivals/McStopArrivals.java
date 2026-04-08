package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals;

import static org.opentripplanner.raptor.api.model.PathLegType.TRANSIT;

import gnu.trove.map.TIntObjectMap;
import java.util.BitSet;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Stream;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.util.BitSetIterator;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;
import org.opentripplanner.raptor.util.paretoset.ParetoSet;
import org.opentripplanner.raptor.util.paretoset.ParetoSetEventListener;

/**
 * This class serve as a wrapper for all stop arrival pareto set, one set for each stop. It also
 * keep track of stops visited since "last mark".
 * <p>
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class McStopArrivals<T extends RaptorTripSchedule> {

  private final ParetoSet<McStopArrival<T>>[] arrivals;
  private final BitSet touchedStops;

  private final DebugHandlerFactory<T> debugHandlerFactory;
  private final DebugStopArrivalsStatistics debugStats;
  private final ParetoComparator<McStopArrival<T>> comparator;

  /**
   * Set the time at a transit index if it is optimal. This sets both the best time and the
   * transfer time.
   *
   * @param nextLegArrivals When chaining two Raptor searches together, the next-leg is the next
   *                search we are copying state into.
   */
  public McStopArrivals(
    int nStops,
    TIntObjectMap<ParetoSetEventListener<ArrivalView<T>>> arrivalListeners,
    ArrivalParetoSetComparatorFactory<McStopArrival<T>> comparatorFactory,
    DebugHandlerFactory<T> debugHandlerFactory
  ) {
    //noinspection unchecked
    this.arrivals = (ParetoSet<McStopArrival<T>>[]) new ParetoSet[nStops];
    this.touchedStops = new BitSet(nStops);
    this.comparator = comparatorFactory.compareArrivalTimeRoundCostAndOnBoardArrival();
    this.debugHandlerFactory = debugHandlerFactory;
    this.debugStats = new DebugStopArrivalsStatistics(debugHandlerFactory.debugLogger());

    for (int stop : arrivalListeners.keys()) {
      this.arrivals[stop] = ParetoSet.of(comparator, arrivalListeners.get(stop));
    }
  }

  public boolean reached(int stopIndex) {
    return arrivals[stopIndex] != null && !arrivals[stopIndex].isEmpty();
  }

  /** Slow! do not use during routing! */
  public int bestArrivalTime(int stopIndex) {
    return minInt(arrivals[stopIndex].stream(), McStopArrival::arrivalTime);
  }

  /** Slow! do not use during routing! */
  public boolean reachedByTransit(int stopIndex) {
    return (
      arrivals[stopIndex] != null &&
      arrivals[stopIndex].stream().anyMatch(a -> a.arrivedBy(TRANSIT))
    );
  }

  /** Slow! do not use during routing! */
  public int bestTransitArrivalTime(int stopIndex) {
    return transitStopArrivalsMinInt(stopIndex, McStopArrival::arrivalTime);
  }

  /** Slow! do not use during routing! */
  public int smallestNumberOfTransfers(int stopIndex) {
    return transitStopArrivalsMinInt(stopIndex, McStopArrival::numberOfTransfers);
  }

  public boolean updateExist() {
    return !touchedStops.isEmpty();
  }

  public IntIterator stopsTouchedIterator() {
    return new BitSetIterator(touchedStops);
  }

  public void addStopArrival(McStopArrival<T> arrival) {
    boolean added = findOrCreateSet(arrival.stop()).add(arrival);

    if (added) {
      touchedStops.set(arrival.stop());
    }
  }

  public void debugStateInfo() {
    debugStats.debugStatInfo(arrivals);
  }

  public boolean hasArrivalsAfterMarker(int stop) {
    var it = arrivals[stop];
    return it != null && it.hasElementsAfterMarker();
  }

  /** List all transits arrived this round. */
  public Iterable<McStopArrival<T>> listArrivalsAfterMarker(final int stop) {
    var it = arrivals[stop];
    // Avoid creating new objects in a tight loop
    return it == null ? Collections::emptyIterator : it.elementsAfterMarker();
  }

  public void clearTouchedStopsAndSetStopMarkers() {
    IntIterator it = stopsTouchedIterator();
    while (it.hasNext()) {
      arrivals[it.next()].markAtEndOfSet();
    }
    touchedStops.clear();
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
