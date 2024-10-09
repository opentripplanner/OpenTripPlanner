package org.opentripplanner.raptor.rangeraptor.multicriteria;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorViaConnection;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrivalFactory;
import org.opentripplanner.raptor.util.paretoset.ParetoSetEventListener;

/**
 * This class is used to listen for stop arrivals in one raptor state and then copy
 * over the arrival event to another state. This is used to chain the Raptor searches
 * together to force the paths through the given via connections.
 */
class ViaConnectionStopArrivalEventListener<T extends RaptorTripSchedule>
  implements ParetoSetEventListener<ArrivalView<T>> {

  private final McStopArrivalFactory<T> stopArrivalFactory;
  private final List<RaptorViaConnection> connections;
  private final McStopArrivals<T> next;

  public ViaConnectionStopArrivalEventListener(
    McStopArrivalFactory<T> stopArrivalFactory,
    List<RaptorViaConnection> connections,
    McStopArrivals<T> next
  ) {
    this.stopArrivalFactory = stopArrivalFactory;
    this.connections = connections;
    this.next = next;
  }

  @Override
  public void notifyElementAccepted(ArrivalView<T> newElement) {
    for (RaptorViaConnection c : connections) {
      var e = (McStopArrival<T>) newElement;
      var n = createViaStopArrival(e, c);
      if (n != null) {
        next.addStopArrival(n);
      }
    }
  }

  @Nullable
  private McStopArrival<T> createViaStopArrival(
    McStopArrival<T> previous,
    RaptorViaConnection viaConnection
  ) {
    if (viaConnection.isSameStop()) {
      if (viaConnection.durationInSeconds() == 0) {
        return previous;
      } else {
        return previous.addSlackToArrivalTime(viaConnection.durationInSeconds());
      }
    } else {
      if (previous.arrivedOnBoard()) {
        return stopArrivalFactory.createTransferStopArrival(
          previous,
          viaConnection.transfer(),
          previous.arrivalTime() + viaConnection.durationInSeconds()
        );
      } else {
        return null;
      }
    }
  }
}
