package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals;

import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.view.PatternRideView;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

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
}
