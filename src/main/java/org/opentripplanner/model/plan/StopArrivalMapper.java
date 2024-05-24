package org.opentripplanner.model.plan;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * Maps leg-related information to an instance of {@link StopArrival}.
 */
class StopArrivalMapper {

  private final ZoneId zoneId;
  private final LocalDate serviceDate;
  private final TripTimes tripTimes;

  public StopArrivalMapper(ZoneId zoneId, LocalDate serviceDate, TripTimes tripTimes) {
    this.zoneId = Objects.requireNonNull(zoneId);
    this.serviceDate = Objects.requireNonNull(serviceDate);
    this.tripTimes = Objects.requireNonNull(tripTimes);
  }

  StopArrival map(int i, StopLocation stop, boolean realTime) {
    final var arrivalTime = ServiceDateUtils.toZonedDateTime(
      serviceDate,
      zoneId,
      tripTimes.getArrivalTime(i)
    );
    final var departureTime = ServiceDateUtils.toZonedDateTime(
      serviceDate,
      zoneId,
      tripTimes.getDepartureTime(i)
    );

    var arrival = LegTime.ofStatic(arrivalTime);
    var departure = LegTime.ofStatic(departureTime);

    if (realTime) {
      arrival = LegTime.of(arrivalTime, tripTimes.getArrivalDelay(i));
      departure = LegTime.of(departureTime, tripTimes.getDepartureDelay(i));
    }

    return new StopArrival(
      Place.forStop(stop),
      arrival,
      departure,
      i,
      tripTimes.gtfsSequenceOfStopIndex(i)
    );
  }
}
