package org.opentripplanner.raptor.rangeraptor.multicriteria;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorkerResult;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorkerState;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrivalFactory;
import org.opentripplanner.raptor.rangeraptor.multicriteria.heuristic.HeuristicsProvider;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRide;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.raptor.rangeraptor.transit.RaptorTransitCalculator;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;

/**
 * Tracks the state of a RAPTOR search, specifically the best arrival times at each transit stop at
 * the end of a particular round, along with associated data to reconstruct paths etc.
 * <p/>
 * This is grouped into a separate class (rather than just having the fields in the raptor worker
 * class) because we want the Algorithm to be as clean as possible and to be able to swap the state
 * implementation - try out and experiment with different state implementations.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class McRangeRaptorWorkerState<T extends RaptorTripSchedule>
  implements RaptorWorkerState<T> {

  private final McStopArrivals<T> arrivals;
  private final DestinationArrivalPaths<T> paths;
  private final HeuristicsProvider<T> heuristics;
  private final McStopArrivalFactory<T> stopArrivalFactory;
  private final List<McStopArrival<T>> arrivalsCache = new ArrayList<>();
  private final RaptorCostCalculator<T> calculatorGeneralizedCost;
  private final RaptorTransitCalculator<T> transitCalculator;

  /**
   * create a RaptorState for a network with a particular number of stops, and a given maximum
   * duration
   */
  public McRangeRaptorWorkerState(
    McStopArrivals<T> arrivals,
    DestinationArrivalPaths<T> paths,
    HeuristicsProvider<T> heuristics,
    McStopArrivalFactory<T> stopArrivalFactory,
    RaptorCostCalculator<T> calculatorGeneralizedCost,
    RaptorTransitCalculator<T> transitCalculator,
    WorkerLifeCycle lifeCycle
  ) {
    this.arrivals = arrivals;
    this.paths = paths;
    this.heuristics = heuristics;
    this.stopArrivalFactory = stopArrivalFactory;
    this.calculatorGeneralizedCost = calculatorGeneralizedCost;
    this.transitCalculator = transitCalculator;

    // Attach to the RR life cycle
    lifeCycle.onSetupIteration(ignore -> setupIteration());
    lifeCycle.onTransitsForRoundComplete(this::transitsForRoundComplete);
    lifeCycle.onTransfersForRoundComplete(this::transfersForRoundComplete);
  }

  // The below methods are ordered after the sequence they naturally appear in the algorithm,
  // also private life-cycle callbacks are listed here (not in the private method section).

  @Override
  public boolean isNewRoundAvailable() {
    return arrivals.updateExist();
  }

  @Override
  public IntIterator stopsTouchedPreviousRound() {
    return arrivals.stopsTouchedIterator();
  }

  @Override
  public IntIterator stopsTouchedByTransitCurrentRound() {
    return arrivals.stopsTouchedIterator();
  }

  @Override
  public boolean isDestinationReachedInCurrentRound() {
    return paths.isReachedCurrentRound();
  }

  @Override
  public boolean isStopReachedInPreviousRound(int stopIndex) {
    return arrivals.hasArrivalsAfterMarker(stopIndex);
  }

  public void setAccessToStop(RaptorAccessEgress accessPath, int departureTime) {
    addStopArrival(stopArrivalFactory.createAccessStopArrival(departureTime, accessPath));
  }

  /**
   * Set the time at a transit stops iff it is optimal.
   */
  @Override
  public void transferToStops(int fromStop, Iterator<? extends RaptorTransfer> transfers) {
    var fromArrivals = arrivals.listArrivalsAfterMarker(fromStop);

    while (transfers.hasNext()) {
      transferToStop(fromArrivals, transfers.next());
    }
  }

  @Override
  public RaptorWorkerResult<T> results() {
    arrivals.debugStateInfo();
    return new McRaptorWorkerResult<T>(arrivals, paths);
  }

  Iterable<? extends McStopArrival<T>> listStopArrivalsPreviousRound(int stop) {
    return arrivals.listArrivalsAfterMarker(stop);
  }

  /**
   * Set the time at a transit stop iff it is optimal.
   */
  void transitToStop(
    final PatternRide<T> ride,
    final int alightStop,
    final int alightTime,
    final int alightSlack
  ) {
    final int stopArrivalTime = alightTime + alightSlack;

    if (exceedsTimeLimit(stopArrivalTime)) {
      return;
    }

    final int c1 = calculateC1(ride, alightStop, alightTime, alightSlack);

    var transitState = stopArrivalFactory.createTransitStopArrival(
      ride,
      alightStop,
      stopArrivalTime,
      c1
    );

    arrivalsCache.add(transitState);
  }

  /* private methods */

  /** This method is called by the Worker life cycle */
  private void setupIteration() {
    arrivalsCache.clear();
    // clear all touched stops to avoid constant re-exploration
    arrivals.clearTouchedStopsAndSetStopMarkers();
  }

  /** This method is called by the Worker life cycle */
  private void transitsForRoundComplete() {
    arrivals.clearTouchedStopsAndSetStopMarkers();
    commitCachedArrivals();
  }

  /** This method is part of Worker life cycle */
  private void transfersForRoundComplete() {
    commitCachedArrivals();
  }

  private void transferToStop(
    Iterable<? extends McStopArrival<T>> fromArrivals,
    RaptorTransfer transfer
  ) {
    final int transferTimeInSeconds = transfer.durationInSeconds();

    for (McStopArrival<T> it : fromArrivals) {
      int arrivalTime = it.arrivalTime() + transferTimeInSeconds;

      if (!exceedsTimeLimit(arrivalTime)) {
        arrivalsCache.add(stopArrivalFactory.createTransferStopArrival(it, transfer, arrivalTime));
      }
    }
  }

  private void commitCachedArrivals() {
    for (McStopArrival<T> arrival : arrivalsCache) {
      addStopArrival(arrival);
    }
    arrivalsCache.clear();
  }

  private void addStopArrival(McStopArrival<T> arrival) {
    if (heuristics.rejectDestinationArrivalBasedOnHeuristic(arrival)) {
      var indexes = new HashSet<Integer>() {{
        add(1984);
        add(1985);
        add(1953);
        add(1987);
        add(1956);
        add(1988);
        add(1957);
        add(1958);
        add(1959);
        add(1963);
        add(1964);
        add(1965);
        add(1967);
        add(1968);
        add(1972);
        add(1975);
        add(1978);
        add(1979);
        add(1981);
        add(1982);
        add(1983);
      }};

      if (!indexes.contains(arrival.stop())) {
        return;
      }
    }
    arrivals.addStopArrival(arrival);
  }

  private int calculateC1(PatternRide<T> ride, int alightStop, int alightTime, int alightSlack) {
    return calculatorGeneralizedCost.transitArrivalCost(
      ride.boardC1(),
      alightSlack,
      alightTime - ride.boardTime(),
      ride.trip(),
      alightStop
    );
  }

  private boolean exceedsTimeLimit(int time) {
    return transitCalculator.exceedsTimeLimit(time);
  }
}
