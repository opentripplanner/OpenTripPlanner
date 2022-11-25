package org.opentripplanner.raptor._data.stoparrival;

import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.api.view.TransitPathView;

public class Bus extends AbstractStopArrival implements TransitPathView<TestTripSchedule> {

  private final TestTripSchedule trip;

  public Bus(
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
  public boolean arrivedByTransit() {
    return true;
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
}
