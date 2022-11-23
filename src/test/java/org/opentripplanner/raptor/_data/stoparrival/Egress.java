package org.opentripplanner.raptor._data.stoparrival;

import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.view.ArrivalView;

public class Egress {

  private final int arrivalTime;
  private final int durationInSeconds;
  private final int cost;
  private final ArrivalView<TestTripSchedule> previous;

  public Egress(
    int departureTime,
    int arrivalTime,
    int cost,
    ArrivalView<TestTripSchedule> previous
  ) {
    this.arrivalTime = arrivalTime;
    this.durationInSeconds = Math.abs(arrivalTime - departureTime);
    this.cost = cost;
    this.previous = previous;
  }

  public int additionalCost() {
    return cost;
  }

  public int durationInSeconds() {
    return durationInSeconds;
  }

  public int arrivalTime() {
    return arrivalTime;
  }

  public ArrivalView<TestTripSchedule> previous() {
    return previous;
  }

  @Override
  public String toString() {
    return String.format(
      "Egress { round: %d, stop: %d, arrival-time: %s $%d }",
      previous.round(),
      previous.stop(),
      TimeUtils.timeToStrCompact(arrivalTime),
      previous.cost() + additionalCost()
    );
  }
}
