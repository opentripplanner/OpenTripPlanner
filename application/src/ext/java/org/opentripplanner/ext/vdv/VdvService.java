package org.opentripplanner.ext.vdv;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.routing.graphfinder.PlaceType;
import org.opentripplanner.routing.stoptimes.ArrivalDeparture;
import org.opentripplanner.transit.model.framework.EntityNotFoundException;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.TransitService;

public class VdvService {

  private final TransitService transitService;
  private final GraphFinder finder;

  public VdvService(TransitService transitService, GraphFinder finder) {
    this.transitService = transitService;
    this.finder = finder;
  }

  public List<TripTimeOnDate> findTripTimesOnDate(
    FeedScopedId stopId,
    Instant time,
    int numResults
  ) throws EntityNotFoundException {
    var stop = transitService.getRegularStop(stopId);
    if (stop == null) {
      throw new EntityNotFoundException("StopPlace", stopId);
    }
    List<StopTimesInPattern> stopTimesInPatterns = transitService.findStopTimesInPattern(
      stop,
      time.atZone(transitService.getTimeZone()).toInstant(),
      Duration.ofHours(2),
      10,
      ArrivalDeparture.BOTH,
      true
    );

    return sort(numResults, stopTimesInPatterns);
  }

  public List<TripTimeOnDate> findTripTimesOnDate(
    WgsCoordinate coordinate,
    Instant time,
    int numResults
  ) {
    var tripTimesOnDate = finder
      .findClosestPlaces(
        coordinate.latitude(),
        coordinate.longitude(),
        1000,
        100,
        null,
        List.of(PlaceType.PATTERN_AT_STOP),
        null,
        null,
        null,
        null,
        null,
        transitService
      )
      .stream()
      .flatMap(p -> {
        if (p.place() instanceof StopTimesInPattern stopTimesInPattern) {
          return Stream.of(stopTimesInPattern);
        } else {
          return Stream.empty();
        }
      })
      .toList();

    return sort(numResults, tripTimesOnDate);
  }

  private static List<TripTimeOnDate> sort(
    int numResults,
    List<StopTimesInPattern> stopTimesInPatterns
  ) {
    return stopTimesInPatterns
      .stream()
      .flatMap(st -> st.times.stream())
      .sorted(Comparator.comparingLong(TripTimeOnDate::getRealtimeDeparture))
      .limit(numResults)
      .toList();
  }
}
