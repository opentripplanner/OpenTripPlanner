package org.opentripplanner.raptor.rangeraptor.multicriteria;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.opentripplanner.raptor.api.path.Path;
import org.opentripplanner.raptor.api.response.StopArrivals;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerState;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.AbstractStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.AccessStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.TransferStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.TransitStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.heuristic.HeuristicsProvider;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.raptor.spi.CostCalculator;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

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
  implements WorkerState<T> {

  private final McStopArrivals<T> arrivals;
  private final DestinationArrivalPaths<T> paths;
  private final HeuristicsProvider<T> heuristics;
  private final List<AbstractStopArrival<T>> arrivalsCache = new ArrayList<>();
  private final CostCalculator<T> costCalculator;
  private final TransitCalculator<T> transitCalculator;

  /**
   * create a RaptorState for a network with a particular number of stops, and a given maximum
   * duration
   */
  public McRangeRaptorWorkerState(
    McStopArrivals<T> arrivals,
    DestinationArrivalPaths<T> paths,
    HeuristicsProvider<T> heuristics,
    CostCalculator<T> costCalculator,
    TransitCalculator<T> transitCalculator,
    WorkerLifeCycle lifeCycle
  ) {
    this.arrivals = arrivals;
    this.paths = paths;
    this.heuristics = heuristics;
    this.costCalculator = costCalculator;
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
  public void setAccessToStop(RaptorAccessEgress accessPath, int departureTime) {
    addStopArrival(new AccessStopArrival<>(departureTime, accessPath));
  }

  /**
   * Set the time at a transit stops iff it is optimal.
   */
  @Override
  public void transferToStops(int fromStop, Iterator<? extends RaptorTransfer> transfers) {
    Iterable<? extends AbstractStopArrival<T>> fromArrivals = arrivals.listArrivalsAfterMarker(
      fromStop
    );

    while (transfers.hasNext()) {
      transferToStop(fromArrivals, transfers.next());
    }
  }

  @Override
  public Collection<Path<T>> extractPaths() {
    arrivals.debugStateInfo();
    return paths.listPaths();
  }

  @Override
  public StopArrivals extractStopArrivals() {
    return arrivals;
  }

  Iterable<? extends AbstractStopArrival<T>> listStopArrivalsPreviousRound(int stop) {
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

    final int costTransit = costCalculator.transitArrivalCost(
      ride.boardCost(),
      alightSlack,
      alightTime - ride.boardTime(),
      ride.trip(),
      alightStop
    );

    arrivalsCache.add(
      new TransitStopArrival<>(
        ride.prevArrival(),
        alightStop,
        stopArrivalTime,
        costTransit,
        ride.trip()
      )
    );
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
    Iterable<? extends AbstractStopArrival<T>> fromArrivals,
    RaptorTransfer transfer
  ) {
    final int transferTimeInSeconds = transfer.durationInSeconds();

    for (AbstractStopArrival<T> it : fromArrivals) {
      int arrivalTime = it.arrivalTime() + transferTimeInSeconds;

      if (!exceedsTimeLimit(arrivalTime)) {
        arrivalsCache.add(new TransferStopArrival<>(it, transfer, arrivalTime));
      }
    }
  }

  private void commitCachedArrivals() {
    for (AbstractStopArrival<T> arrival : arrivalsCache) {
      addStopArrival(arrival);
    }
    arrivalsCache.clear();
  }

  private void addStopArrival(AbstractStopArrival<T> arrival) {
    if (heuristics.rejectDestinationArrivalBasedOnHeuristic(arrival)) {
      return;
    }
    arrivals.addStopArrival(arrival);
  }

  private boolean exceedsTimeLimit(int time) {
    return transitCalculator.exceedsTimeLimit(time);
  }
}
