package org.opentripplanner.raptor.rangeraptor.standard.stoparrivals;

import java.util.Collection;
import org.opentripplanner.raptor.api.path.Path;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.raptor.rangeraptor.standard.internalapi.StopArrivalsState;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.TransitArrival;

/**
 * Tracks the state necessary to construct paths at the end of each iteration.
 * <p/>
 * This class find the pareto optimal paths with respect to: rounds, arrival time and total travel
 * time.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class StdStopArrivalsState<T extends RaptorTripSchedule>
  implements StopArrivalsState<T> {

  private final StdStopArrivals<T> stops;
  private final DestinationArrivalPaths<T> results;

  /**
   * Create a Standard Range Raptor state for the given stops and destination arrivals.
   */
  public StdStopArrivalsState(StdStopArrivals<T> stops, DestinationArrivalPaths<T> paths) {
    this.stops = stops;
    this.results = paths;
  }

  @Override
  public void setAccessTime(int arrivalTime, RaptorAccessEgress access, boolean bestTime) {
    stops.setAccessTime(arrivalTime, access, bestTime);
  }

  @Override
  public int bestTimePreviousRound(int stop) {
    return stops.bestTimePreviousRound(stop);
  }

  @Override
  public void setNewBestTransitTime(
    int stop,
    int alightTime,
    T trip,
    int boardStop,
    int boardTime,
    boolean newBestOverall
  ) {
    stops.transitToStop(stop, alightTime, boardStop, boardTime, trip, newBestOverall);
  }

  @Override
  public void setNewBestTransferTime(int fromStop, int arrivalTime, RaptorTransfer transfer) {
    stops.transferToStop(fromStop, transfer, arrivalTime);
  }

  @Override
  public TransitArrival<T> previousTransit(int boardStopIndex) {
    return stops.previousTransit(boardStopIndex);
  }

  @Override
  public Collection<Path<T>> extractPaths() {
    return results.listPaths();
  }

  @Override
  public int calculateMinNumberOfTransfers(int stopIndex) {
    return stops.calculateMinNumberOfTransfers(stopIndex);
  }
}
