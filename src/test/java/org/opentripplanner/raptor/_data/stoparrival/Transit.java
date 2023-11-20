package org.opentripplanner.raptor._data.stoparrival;

import static org.opentripplanner.raptor.api.model.PathLegType.TRANSIT;

import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.PathLegType;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.api.view.TransitPathView;

class Transit extends AbstractStopArrival implements TransitPathView<TestTripSchedule> {

  private final TestTripSchedule trip;

  Transit(
    int round,
    int stop,
    int arrivalTime,
    int cost,
    TestTripSchedule trip,
    ArrivalView<TestTripSchedule> previous
  ) {
    super(round, stop, arrivalTime, cost, previous);
    this.trip = trip;
  }

  @Override
  public PathLegType arrivedBy() {
    return TRANSIT;
  }

  @Override
  public TransitPathView<TestTripSchedule> transitPath() {
    return this;
  }

  @Override
  public int boardStop() {
    return previous().stop();
  }

  @Override
  public TestTripSchedule trip() {
    return trip;
  }

  @Override
  public boolean arrivedOnBoard() {
    return true;
  }
}
