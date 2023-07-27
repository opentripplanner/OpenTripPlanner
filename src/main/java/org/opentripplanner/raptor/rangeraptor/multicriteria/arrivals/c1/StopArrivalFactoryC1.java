package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.c1;

import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.view.PatternRideView;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrivalFactory;

/**
 * This class is responsible for creating StopArrivals which support accumulated criteria ONE.
 */
public class StopArrivalFactoryC1<T extends RaptorTripSchedule> implements McStopArrivalFactory<T> {

  @Override
  public McStopArrival<T> createAccessStopArrival(
    int departureTime,
    RaptorAccessEgress accessPath
  ) {
    return new AccessStopArrival<>(departureTime, accessPath);
  }

  @Override
  public McStopArrival<T> createTransitStopArrival(
    PatternRideView<T, McStopArrival<T>> ride,
    int alightStop,
    int stopArrivalTime,
    int c1
  ) {
    return new TransitStopArrival<>(
      ride.prevArrival(),
      alightStop,
      stopArrivalTime,
      c1,
      ride.trip()
    );
  }

  @Override
  public McStopArrival<T> createTransferStopArrival(
    McStopArrival<T> previous,
    RaptorTransfer transfer,
    int arrivalTime
  ) {
    return new TransferStopArrival<>(previous, transfer, arrivalTime);
  }
}
