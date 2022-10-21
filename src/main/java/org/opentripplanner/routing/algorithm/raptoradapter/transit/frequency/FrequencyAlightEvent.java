package org.opentripplanner.routing.algorithm.raptoradapter.transit.frequency;

import java.time.LocalDate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.DefaultTripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TripPatternForDates;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * Represents a result of a {@link TripFrequencyAlightSearch}, with materialized {@link TripTimes}
 */
final class FrequencyAlightEvent<T extends DefaultTripSchedule>
  extends FrequencyBoardOrAlightEvent<T> {

  public FrequencyAlightEvent(
    TripPatternForDates raptorTripPattern,
    TripTimes tripTimes,
    int stopPositionInPattern,
    int departureTime,
    int headway,
    int offset,
    LocalDate serviceDate
  ) {
    super(
      raptorTripPattern,
      tripTimes,
      stopPositionInPattern,
      departureTime,
      offset,
      headway,
      serviceDate
    );
  }

  @Override
  public int arrival(int stopPosInPattern) {
    return tripTimes.getArrivalTime(stopPosInPattern) + offset;
  }

  // Remove headway here to report an early enough departure time for the raptor search
  @Override
  public int departure(int stopPosInPattern) {
    return tripTimes.getDepartureTime(stopPosInPattern) - headway + offset;
  }
}
