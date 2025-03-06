package org.opentripplanner.raptor.rangeraptor.standard.stoparrivals;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.standard.internalapi.DestinationArrivalListener;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The egress stop arrival state is responsible for sending arrival notifications. This is used to
 * update the destination arrivals.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
final class EgressStopArrivalState<T extends RaptorTripSchedule>
  extends DefaultStopArrivalState<T> {

  private final int round;
  private final int stop;
  private final RaptorAccessEgress[] egressPaths;
  private final DestinationArrivalListener callback;

  EgressStopArrivalState(
    int stop,
    int round,
    Collection<RaptorAccessEgress> egressPaths,
    DestinationArrivalListener transitCallback
  ) {
    this.round = round;
    this.stop = stop;
    this.egressPaths = egressPaths.toArray(new RaptorAccessEgress[0]);
    this.callback = transitCallback;
  }

  public int round() {
    return round;
  }

  public int stop() {
    return stop;
  }

  @Override
  public void arriveByTransit(int arrivalTime, int boardStop, int boardTime, T trip) {
    super.arriveByTransit(arrivalTime, boardStop, boardTime, trip);
    for (RaptorAccessEgress egressPath : egressPaths) {
      callback.newDestinationArrival(round, arrivalTime, true, egressPath);
    }
  }

  @Override
  public void transferToStop(int fromStop, int arrivalTime, RaptorTransfer transferPath) {
    super.transferToStop(fromStop, arrivalTime, transferPath);
    for (RaptorAccessEgress egressPath : egressPaths) {
      if (egressPath.stopReachedOnBoard()) {
        // Raptor does not support currently egress directly after flex access.
        // There has to be at least one transit in between.
        // Hence, stopReachedOnBoard=false
        callback.newDestinationArrival(round, arrivalTime, false, egressPath);
      }
    }
  }

  @Override
  void setAccessTime(int time, boolean isBestTimeOverall, boolean onBoard) {
    super.setAccessTime(time, isBestTimeOverall, onBoard);
    for (RaptorAccessEgress egressPath : egressPaths) {
      callback.newDestinationArrival(round, time, onBoard, egressPath);
    }
  }

  @Override
  public String toString() {
    var builder = ToStringBuilder.of(EgressStopArrivalState.class)
      .addNum("stop", stop)
      .addNum("round", round);
    // Add super type fields
    toStringAddBody(builder);
    // Add egress stop last (collection)
    builder.addCol("egressPaths", List.of(egressPaths));
    return builder.toString();
  }
}
