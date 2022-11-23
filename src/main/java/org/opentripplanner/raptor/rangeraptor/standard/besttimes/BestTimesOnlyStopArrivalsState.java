package org.opentripplanner.raptor.rangeraptor.standard.besttimes;

import org.opentripplanner.raptor.rangeraptor.standard.internalapi.StopArrivalsState;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.TransitArrival;

/**
 * The responsibility of this class is to calculate the best arrival times at every stop. This class
 * do NOT keep track of results paths.
 * <p/>
 * The {@link #bestTimePreviousRound(int)} return an estimate of the best time for the previous
 * round by using the overall best time (any round including the current round).
 * <p/>
 * This class is used to calculate heuristic information like the best possible arrival times and
 * the minimum number for transfers. The results are an optimistic "guess", since we use the
 * overall best time instead of best time previous round we might skip hops.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class BestTimesOnlyStopArrivalsState<T extends RaptorTripSchedule>
  implements StopArrivalsState<T> {

  private final BestTimes bestTimes;
  private final SimpleBestNumberOfTransfers bestNumberOfTransfers;

  public BestTimesOnlyStopArrivalsState(
    BestTimes bestTimes,
    SimpleBestNumberOfTransfers bestNumberOfTransfers
  ) {
    this.bestTimes = bestTimes;
    this.bestNumberOfTransfers = bestNumberOfTransfers;
  }

  @Override
  public void setAccessTime(int arrivalTime, RaptorAccessEgress access, boolean bestTime) {
    bestNumberOfTransfers.arriveAtStop(access.stop());
  }

  /**
   * This implementation does NOT return the "best time in the previous round"; It returns the
   * overall "best time" across all rounds including the current.
   * <p/>
   * This is a simplification, *bestTimes* might get updated during the current round; Hence leading
   * to a new boarding at the alight stop in the same round. If we do not count rounds or track
   * paths, this is OK.
   * <p/>
   * Because this rarely happens and heuristics does not need to be exact - it only need to be
   * optimistic. So if we arrive at a stop one or two rounds to early, the only effect is that the
   * "number of transfers" for those stops is to small - or what we call a optimistic estimate.
   * <p/>
   * The "arrival time" is calculated correctly.
   */
  @Override
  public int bestTimePreviousRound(int stop) {
    return bestTimes.time(stop);
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
    bestNumberOfTransfers.arriveAtStop(stop);
  }

  @Override
  public void setNewBestTransferTime(int fromStop, int arrivalTime, RaptorTransfer transfer) {
    bestNumberOfTransfers.arriveAtStop(transfer.stop());
  }

  @Override
  public TransitArrival<T> previousTransit(int boardStopIndex) {
    throw new IllegalStateException(
      "The implementation of this interface is not compatible with the request" +
      "configuration. For example the BestTimesOnlyStopArrivalsState can not be used " +
      "with constrained transfers."
    );
  }

  @Override
  public int calculateMinNumberOfTransfers(int stopIndex) {
    return bestNumberOfTransfers.calculateMinNumberOfTransfers(stopIndex);
  }
}
