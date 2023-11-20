package org.opentripplanner.raptor._data.stoparrival;

import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.view.ArrivalView;

abstract class AbstractStopArrival implements ArrivalView<TestTripSchedule> {

  private final int round;
  private final int stop;
  private final int arrivalTime;
  private final int c1;
  private final int c2;
  private final ArrivalView<TestTripSchedule> previous;

  AbstractStopArrival(
    int round,
    int stop,
    int arrivalTime,
    int extraCost,
    int c2,
    ArrivalView<TestTripSchedule> previous
  ) {
    this.round = round;
    this.stop = stop;
    this.arrivalTime = arrivalTime;
    this.previous = previous;

    if (previous == null) {
      this.c1 = extraCost;
      this.c2 = c2;
    } else {
      this.c1 = previous.c1() + extraCost;
      this.c2 = c2 == RaptorConstants.NOT_SET ? previous.c2() : c2;
    }
  }

  AbstractStopArrival(
    int round,
    int stop,
    int arrivalTime,
    int extraCost,
    ArrivalView<TestTripSchedule> previous
  ) {
    this(round, stop, arrivalTime, extraCost, RaptorConstants.NOT_SET, previous);
  }

  @Override
  public int stop() {
    return stop;
  }

  @Override
  public int round() {
    return round;
  }

  @Override
  public int arrivalTime() {
    return arrivalTime;
  }

  @Override
  public int c1() {
    return c1;
  }

  @Override
  public int c2() {
    return c2;
  }

  @Override
  public boolean supportsC2() {
    return c2 != RaptorConstants.NOT_SET;
  }

  @Override
  public ArrivalView<TestTripSchedule> previous() {
    return previous;
  }

  @Override
  public String toString() {
    return asString();
  }
}
