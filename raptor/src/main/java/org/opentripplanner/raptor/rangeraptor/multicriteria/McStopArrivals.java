package org.opentripplanner.raptor.rangeraptor.multicriteria;

import static org.opentripplanner.raptor.api.model.PathLegType.TRANSIT;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.ArrivalParetoSetComparatorFactory;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.raptor.rangeraptor.transit.EgressPaths;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.util.BitSetIterator;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;

/**
 * This class serve as a wrapper for all stop arrival pareto set, one set for each stop. It also
 * keep track of stops visited since "last mark".
 * <p>
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class McStopArrivals<T extends RaptorTripSchedule> {

  private final StopArrivalParetoSet<T>[] arrivals;
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
    @Nullable EgressPaths egressPaths,
    List<ViaConnectionStopArrivalEventListener<T>> viaConnectionListeners,
    DestinationArrivalPaths<T> paths,
    ArrivalParetoSetComparatorFactory<McStopArrival<T>> comparatorFactory,
    DebugHandlerFactory<T> debugHandlerFactory
  ) {
    // Assert only-one-of next or egressPaths is set
    if (viaConnectionListeners.isEmpty()) {
      Objects.requireNonNull(egressPaths);
    } else if (egressPaths != null) {
      throw new IllegalArgumentException(
        "Can not delegate to next-leg and at the same have egress paths."
      );
    }

    //noinspection unchecked
    this.arrivals = (StopArrivalParetoSet<T>[]) new StopArrivalParetoSet[nStops];
    this.touchedStops = new BitSet(nStops);
    this.comparator = comparatorFactory.compareArrivalTimeRoundCostAndOnBoardArrival();
    this.debugHandlerFactory = debugHandlerFactory;
    this.debugStats = new DebugStopArrivalsStatistics(debugHandlerFactory.debugLogger());

    initViaConnections(viaConnectionListeners);
    initEgressStopAndGlueItToDestinationArrivals(egressPaths, paths);
  }

  boolean reached(int stopIndex) {
    return arrivals[stopIndex] != null && !arrivals[stopIndex].isEmpty();
  }

  /** Slow! do not use during routing! */
  int bestArrivalTime(int stopIndex) {
    return minInt(arrivals[stopIndex].stream(), McStopArrival::arrivalTime);
  }

  boolean reachedByTransit(int stopIndex) {
    return (
      arrivals[stopIndex] != null &&
      arrivals[stopIndex].stream().anyMatch(a -> a.arrivedBy(TRANSIT))
    );
  }

  /** Slow! do not use during routing! */
  int bestTransitArrivalTime(int stopIndex) {
    return transitStopArrivalsMinInt(stopIndex, McStopArrival::arrivalTime);
  }

  /** Slow! do not use during routing! */
  int smallestNumberOfTransfers(int stopIndex) {
    return transitStopArrivalsMinInt(stopIndex, McStopArrival::numberOfTransfers);
  }

  boolean updateExist() {
    return !touchedStops.isEmpty();
  }

  IntIterator stopsTouchedIterator() {
    return new BitSetIterator(touchedStops);
  }

  void addStopArrival(McStopArrival<T> arrival) {
    boolean added = findOrCreateSet(arrival.stop()).add(arrival);

    if (added) {
      touchedStops.set(arrival.stop());
    }
  }

  void debugStateInfo() {
    debugStats.debugStatInfo(arrivals);
  }

  boolean hasArrivalsAfterMarker(int stop) {
    var it = arrivals[stop];
    return it != null && it.hasElementsAfterMarker();
  }

  /** List all transits arrived this round. */
  Iterable<McStopArrival<T>> listArrivalsAfterMarker(final int stop) {
    var it = arrivals[stop];
    // Avoid creating new objects in a tight loop
    return it == null ? Collections::emptyIterator : it.elementsAfterMarker();
  }

  void clearTouchedStopsAndSetStopMarkers() {
    IntIterator it = stopsTouchedIterator();
    while (it.hasNext()) {
      arrivals[it.next()].markAtEndOfSet();
    }
    touchedStops.clear();
  }

  /* private methods */

  private StopArrivalParetoSet<T> findOrCreateSet(final int stop) {
    if (arrivals[stop] == null) {
      arrivals[stop] = StopArrivalParetoSet.of(comparator)
        .withDebugListener(debugHandlerFactory.paretoSetStopArrivalListener(stop))
        .build();
    }
    return arrivals[stop];
  }

  private void initViaConnections(
    List<ViaConnectionStopArrivalEventListener<T>> viaConnectionListeners
  ) {
    for (ViaConnectionStopArrivalEventListener<T> it : viaConnectionListeners) {
      int stop = it.fromStop();
      this.arrivals[stop] = StopArrivalParetoSet.of(comparator)
        .withDebugListener(debugHandlerFactory.paretoSetStopArrivalListener(stop))
        .withNextLegListener(it)
        .build();
    }
  }

  /**
   * This method creates a ParetoSet for the given egress stop. When arrivals are added to the stop,
   * the "glue" make sure new destination arrivals are added to the destination arrivals.
   */
  private void initEgressStopAndGlueItToDestinationArrivals(
    @Nullable EgressPaths egressPaths,
    DestinationArrivalPaths<T> paths
  ) {
    if (egressPaths == null) {
      return;
    }

    egressPaths
      .byStop()
      .forEachEntry((stop, list) -> {
        // The factory is creating the actual "glue"
        this.arrivals[stop] = StopArrivalParetoSet.of(comparator)
          .withDebugListener(debugHandlerFactory.paretoSetStopArrivalListener(stop))
          .withEgressListener(list, paths)
          .build();
        return true;
      });
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
