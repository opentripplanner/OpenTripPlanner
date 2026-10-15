package org.opentripplanner.raptor.rangeraptor.internalapi;

import org.opentripplanner.raptor.api.model.RaptorTripScheduleStopPosition;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

public class OnTripAccessArrival<T extends RaptorTripSchedule> {

  private final ArrivalView<T> accessStopArrival;
  private final RaptorTripScheduleStopPosition boardingConstraint;

  public OnTripAccessArrival(
    ArrivalView<T> accessStopArrival,
    RaptorTripScheduleStopPosition boardingConstraint
  ) {
    this.accessStopArrival = accessStopArrival;
    this.boardingConstraint = boardingConstraint;
  }

  public ArrivalView<T> accessStopArrival() {
    return accessStopArrival;
  }

  public RaptorTripScheduleStopPosition boardingConstraint() {
    return boardingConstraint;
  }
}
