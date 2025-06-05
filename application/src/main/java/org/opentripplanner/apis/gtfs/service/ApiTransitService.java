package org.opentripplanner.apis.gtfs.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.ArrivalDeparture;
import org.opentripplanner.transit.service.TransitService;

/**
 * A service for methods that are use case-specific to the GTFS GraphQL API.
 */
public class ApiTransitService {

  private final TransitService transitService;

  public ApiTransitService(TransitService transitService) {
    this.transitService = transitService;
  }

  /**
   * Domain-specific method to get a list of {@link TripTimeOnDate} for a given stop in a pattern.
   */
  public List<TripTimeOnDate> getTripTimeOnDatesForPatternAtStopIncludingTripsWithSkippedStops(
    StopLocation stop,
    TripPattern originalPattern,
    Instant startTime,
    Duration timeRange,
    int numDepartures,
    ArrivalDeparture arrivalDeparture
  ) {
    LocalDate date = startTime.atZone(transitService.getTimeZone()).toLocalDate();

    return Stream.concat(
      getRealtimeAddedPatternsAsStream(originalPattern, date),
      Stream.of(originalPattern)
    )
      .distinct()
      .flatMap(tripPattern ->
        transitService
          .findTripTimesOnDate(
            stop,
            tripPattern,
            startTime,
            timeRange,
            numDepartures,
            arrivalDeparture,
            false
          )
          .stream()
      )
      .sorted(
        Comparator.comparing(
          (TripTimeOnDate tts) -> tts.getServiceDayMidnight() + tts.getRealtimeDeparture()
        )
      )
      .limit(numDepartures)
      .toList();
  }

  /**
   * Get a stream of {@link TripPattern} that were created real-time based of the provided pattern.
   * Only patterns that don't have removed (stops can still be skipped) or added stops are included.
   */
  private Stream<TripPattern> getRealtimeAddedPatternsAsStream(
    TripPattern originalPattern,
    LocalDate date
  ) {
    return originalPattern
      .scheduledTripsAsStream()
      .map(trip -> transitService.findNewTripPatternForModifiedTrip(trip.getId(), date))
      .filter(
        tripPattern ->
          tripPattern != null &&
          tripPattern.isModifiedFromTripPatternWithEqualStops(originalPattern)
      );
  }
}
