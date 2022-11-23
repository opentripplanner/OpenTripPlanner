package org.opentripplanner.raptor.rangeraptor.multicriteria;

import java.util.BitSet;
import java.util.Collections;
import org.opentripplanner.raptor.api.response.StopArrivals;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.AbstractStopArrival;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.raptor.rangeraptor.transit.EgressPaths;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.util.BitSetIterator;

/**
 * This class serve as a wrapper for all stop arrival pareto set, one set for each stop. It also
 * keep track of stops visited since "last mark".
 * <p>
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class McStopArrivals<T extends RaptorTripSchedule> implements StopArrivals {

  private final StopArrivalParetoSet<T>[] arrivals;
  private final BitSet touchedStops;
  private final DebugHandlerFactory<T> debugHandlerFactory;
  private final DebugStopArrivalsStatistics debugStats;

  /**
   * Set the time at a transit index iff it is optimal. This sets both the best time and the
   * transfer time
   */
  public McStopArrivals(
    int nStops,
    EgressPaths egressPaths,
    DestinationArrivalPaths<T> paths,
    DebugHandlerFactory<T> debugHandlerFactory
  ) {
    //noinspection unchecked
    this.arrivals = (StopArrivalParetoSet<T>[]) new StopArrivalParetoSet[nStops];
    this.touchedStops = new BitSet(nStops);
    this.debugHandlerFactory = debugHandlerFactory;
    this.debugStats = new DebugStopArrivalsStatistics(debugHandlerFactory.debugLogger());

    glueTogetherEgressStopWithDestinationArrivals(egressPaths, paths);
  }

  @Override
  public boolean reached(int stopIndex) {
    return arrivals[stopIndex] != null && !arrivals[stopIndex].isEmpty();
  }

  @Override
  public int bestArrivalTime(int stopIndex) {
    return arrivals[stopIndex].stream()
      .mapToInt(AbstractStopArrival::arrivalTime)
      .min()
      .orElseThrow();
  }

  @Override
  public boolean reachedByTransit(int stopIndex) {
    return (
      arrivals[stopIndex] != null &&
      arrivals[stopIndex].stream().anyMatch(ArrivalView::arrivedByTransit)
    );
  }

  @Override
  public int bestTransitArrivalTime(int stopIndex) {
    return arrivals[stopIndex].stream()
      .filter(ArrivalView::arrivedByTransit)
      .mapToInt(AbstractStopArrival::arrivalTime)
      .min()
      .orElseThrow();
  }

  @Override
  public int smallestNumberOfTransfers(int stopIndex) {
    return arrivals[stopIndex].stream()
      .filter(ArrivalView::arrivedByTransit)
      .mapToInt(AbstractStopArrival::numberOfTransfers)
      .min()
      .orElseThrow();
  }

  boolean updateExist() {
    return !touchedStops.isEmpty();
  }

  IntIterator stopsTouchedIterator() {
    return new BitSetIterator(touchedStops);
  }

  void addStopArrival(AbstractStopArrival<T> arrival) {
    boolean added = findOrCreateSet(arrival.stop()).add(arrival);
    if (added) {
      touchedStops.set(arrival.stop());
    }
  }

  void debugStateInfo() {
    debugStats.debugStatInfo(arrivals);
  }

  /** List all transits arrived this round. */
  Iterable<AbstractStopArrival<T>> listArrivalsAfterMarker(final int stop) {
    StopArrivalParetoSet<T> it = arrivals[stop];
    if (it == null) {
      // Avoid creating new objects in a tight loop
      return Collections::emptyIterator;
    }
    return it.elementsAfterMarker();
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
      arrivals[stop] = StopArrivalParetoSet.createStopArrivalSet(stop, debugHandlerFactory);
    }
    return arrivals[stop];
  }

  /**
   * This method creates a ParetoSet for the given egress stop. When arrivals are added to the stop,
   * the "glue" make sure new destination arrivals is added to the destination arrivals.
   */
  private void glueTogetherEgressStopWithDestinationArrivals(
    EgressPaths egressPaths,
    DestinationArrivalPaths<T> paths
  ) {
    egressPaths
      .byStop()
      .forEachEntry((stop, list) -> {
        // The factory is creating the actual "glue"
        this.arrivals[stop] =
          StopArrivalParetoSet.createEgressStopArrivalSet(stop, list, paths, debugHandlerFactory);
        return true;
      });
  }
}
