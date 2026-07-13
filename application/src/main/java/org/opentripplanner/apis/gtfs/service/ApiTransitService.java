package org.opentripplanner.apis.gtfs.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.opentripplanner.apis.gtfs.model.StopCallOnTripOnServiceDate;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.transit.api.request.CancellationPolicy;
import org.opentripplanner.transit.api.request.TripTimeOnDateRequest;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
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
   * Find the stop calls of the leg. Note that this includes the boarding and alighting calls.
   */
  public List<TripTimeOnDate> findStopCalls(Leg leg) {
    if (leg.isTransitLeg()) {
      var calls = transitService
        .findTripTimesOnDate(leg.trip(), leg.serviceDate())
        .orElseThrow(() ->
          new IllegalStateException(
            "Cannot find times for %s on service date %s".formatted(leg.trip(), leg.serviceDate())
          )
        );
      return calls.subList(leg.boardStopPosInPattern(), leg.alightStopPosInPattern() + 1);
    } else {
      return List.of();
    }
  }

  /**
   * Find the canceled stop calls at the given stop. A call is included if either the trip it
   * belongs to has been canceled, or the visit at this stop has been canceled (skipped). Only calls
   * whose trip's service date is within any of the given service date ranges are returned. Each
   * call is paired with the {@link TripOnServiceDate} it belongs to, which is synthesized when no
   * real one exists.
   */
  public List<StopCallOnTripOnServiceDate> findCanceledStopCalls(
    StopLocation stop,
    List<LocalDateRange> serviceDateRanges
  ) {
    var request = TripTimeOnDateRequest.of(List.of(stop))
      .withServiceDateRanges(serviceDateRanges)
      .withArrivalDeparture(ArrivalDeparture.BOTH)
      .withNumberOfDepartures(Integer.MAX_VALUE)
      .withCancellationPolicy(CancellationPolicy.ONLY_CANCELLATIONS)
      .build();

    return transitService
      .findTripTimesOnDate(request)
      .stream()
      .map(call -> new StopCallOnTripOnServiceDate(resolveTripOnServiceDate(call), call))
      .toList();
  }

  /**
   * Resolve the {@link TripOnServiceDate} for a stop call, synthesizing one from the trip and
   * service date when no real one exists (for example for GTFS-sourced data).
   */
  private TripOnServiceDate resolveTripOnServiceDate(TripTimeOnDate call) {
    Trip trip = call.getTrip();
    LocalDate serviceDate = call.getServiceDay();
    var tripOnServiceDate = transitService.getTripOnServiceDate(
      new TripIdAndServiceDate(trip.getId(), serviceDate)
    );
    if (tripOnServiceDate != null) {
      return tripOnServiceDate;
    }
    return TripOnServiceDate.of(trip.getId()).withTrip(trip).withServiceDate(serviceDate).build();
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
