package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop;

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

  /**
   * Create an arrival for a regular transfer. The transfer is not kept; only the target stop and
   * the cost are copied, so it is safe to pass in a flyweight.
   */
  McStopArrival<T> createTransferStopArrival(
    McStopArrival<T> previous,
    RaptorTransfer transfer,
    int arrivalTime
  );

  /** Create an arrival for the transfer of a via connection. The transfer is kept. */
  McStopArrival<T> createViaTransferStopArrival(
    McStopArrival<T> previous,
    RaptorTransfer viaTransfer,
    int arrivalTime
  );
}
