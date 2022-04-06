package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.WheelChairBoarding;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;

public record TestTripSchedule(WheelChairBoarding wheelchairBoarding) implements TripSchedule {
  @Override
  public TripTimes getOriginalTripTimes() {
    var trip = new Trip(new FeedScopedId("1", "1"));
    var stopTime = new StopTime();
    stopTime.setArrivalTime(100);
    trip.setWheelchairBoarding(wheelchairBoarding);
    return new TripTimes(trip, List.of(stopTime), new Deduplicator());
  }

  @Override
  public TripPattern getOriginalTripPattern() {
    return null;
  }

  @Override
  public LocalDate getServiceDate() {
    return null;
  }

  @Override
  public int tripSortIndex() {
    return 0;
  }

  @Override
  public int arrival(int stopPosInPattern) {
    return 0;
  }

  @Override
  public int departure(int stopPosInPattern) {
    return 0;
  }

  @Override
  public RaptorTripPattern pattern() {
    return null;
  }

  @Override
  public int transitReluctanceFactorIndex() {
    return 0;
  }
}
