package org.opentripplanner.raptor.rangeraptor.multicriteria;

import static org.opentripplanner.raptor.api.model.PathLegType.TRANSIT;

import gnu.trove.map.TIntObjectMap;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.AbstractStopArrival;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.raptor.rangeraptor.transit.AccessPaths;
import org.opentripplanner.raptor.rangeraptor.transit.EgressPaths;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.util.BitSetIterator;

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

  /**
   * Set the time at a transit index iff it is optimal. This sets both the best time and the
   * transfer time
   */
  public McStopArrivals(
    int nStops,
    EgressPaths egressPaths,
    AccessPaths accessPaths,
    DestinationArrivalPaths<T> paths,
    DebugHandlerFactory<T> debugHandlerFactory
  ) {
    //noinspection unchecked
    this.arrivals = (StopArrivalParetoSet<T>[]) new StopArrivalParetoSet[nStops];
    this.touchedStops = new BitSet(nStops);
    this.debugHandlerFactory = debugHandlerFactory;
    this.debugStats = new DebugStopArrivalsStatistics(debugHandlerFactory.debugLogger());

    initAccessArrivals(accessPaths.arrivedOnBoardByNumOfRides());
    glueTogetherEgressStopWithDestinationArrivals(egressPaths, paths);
  }

  public boolean reached(int stopIndex) {
    return arrivals[stopIndex] != null && !arrivals[stopIndex].isEmpty();
  }

  public int bestArrivalTime(int stopIndex) {
    return arrivals[stopIndex].stream()
      .mapToInt(AbstractStopArrival::arrivalTime)
      .min()
      .orElseThrow();
  }

  public boolean reachedByTransit(int stopIndex) {
    return (
      arrivals[stopIndex] != null &&
      arrivals[stopIndex].stream().anyMatch(a -> a.arrivedBy(TRANSIT))
    );
  }

  public int bestTransitArrivalTime(int stopIndex) {
    return stopArrivalsByTransit(stopIndex)
      .mapToInt(AbstractStopArrival::arrivalTime)
      .min()
      .orElseThrow();
  }

  public int smallestNumberOfTransfers(int stopIndex) {
    return stopArrivalsByTransit(stopIndex)
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

  public boolean hasArrivalsAfterMarker(int stop) {
    StopArrivalParetoSet<T> it = arrivals[stop];
    if (it == null) {
      return false;
    }
    return it.hasElementsAfterMarker();
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
      arrivals[stop] =
        StopArrivalParetoSet.createStopArrivalSet(
          debugHandlerFactory.paretoSetStopArrivalListener(stop)
        );
    }
    return arrivals[stop];
  }

  private void initAccessArrivals(TIntObjectMap<List<RaptorAccessEgress>> accessOnBoardByRides) {
    for (int round : accessOnBoardByRides.keys()) {
      for (var access : accessOnBoardByRides.get(round)) {
        int stop = access.stop();
        arrivals[stop] =
          StopArrivalParetoSet.createStopArrivalSetWithOnBoardCriteria(
            debugHandlerFactory.paretoSetStopArrivalListener(stop)
          );
      }
    }
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
          StopArrivalParetoSet.createEgressStopArrivalSet(
            list,
            paths,
            debugHandlerFactory.paretoSetStopArrivalListener(stop)
          );
        return true;
      });
  }

  @Nonnull
  private Stream<AbstractStopArrival<T>> stopArrivalsByTransit(int stopIndex) {
    return arrivals[stopIndex].stream().filter(a -> a.arrivedBy(TRANSIT));
  }
}
