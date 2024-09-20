package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals;

import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorViaConnection;
import org.opentripplanner.raptor.api.view.PatternRideView;

public interface McStopArrivalFactory<T extends RaptorTripSchedule> {
  McStopArrival<T> createAccessStopArrival(int departureTime, RaptorAccessEgress accessPath);

  McStopArrival<T> createTransitStopArrival(
    PatternRideView<T, McStopArrival<T>> ride,
    int alightStop,
    int stopArrivalTime,
    int c1
  );

  McStopArrival<T> createTransferStopArrival(
    McStopArrival<T> previous,
    RaptorTransfer transfer,
    int arrivalTime
  );

  default McStopArrival<T> createViaStopArrival(
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
        return createTransferStopArrival(
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
