package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.c2;

import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrivalFactory;

public class StopArrivalFactoryC2<T extends RaptorTripSchedule> implements McStopArrivalFactory<T> {

  @Override
  public McStopArrival<T> createAccessStopArrival(
    int departureTime,
    RaptorAccessEgress accessPath
  ) {
    return new AccessStopArrivalC2<>(departureTime, accessPath);
  }

  @Override
  public McStopArrival<T> createTransitStopArrival(
    McStopArrival<T> previous,
    int alightStop,
    int stopArrivalTime,
    int c1,
    T trip
  ) {
    return new TransitStopArrivalC2<>(previous, alightStop, stopArrivalTime, c1, trip);
  }

  @Override
  public McStopArrival<T> createTransferStopArrival(
    McStopArrival<T> previous,
    RaptorTransfer transfer,
    int arrivalTime
  ) {
    return new TransferStopArrivalC2<>(previous, transfer, arrivalTime);
  }
}
