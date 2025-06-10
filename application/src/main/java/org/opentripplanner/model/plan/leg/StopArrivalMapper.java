package org.opentripplanner.model.plan.leg;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.utils.time.ServiceDateUtils;

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

    var arrival = LegCallTime.ofStatic(arrivalTime);
    var departure = LegCallTime.ofStatic(departureTime);

    if (realTime) {
      arrival = LegCallTime.of(arrivalTime, tripTimes.getArrivalDelay(i));
      departure = LegCallTime.of(departureTime, tripTimes.getDepartureDelay(i));
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
