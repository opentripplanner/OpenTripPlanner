package org.opentripplanner.ext.vdv;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.routing.graphfinder.PatternAtStop;
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

  public List<CallAtStop> findTripTimesOnDate(FeedScopedId stopId, Instant time, int numResults)
    throws EntityNotFoundException {
    var stop = transitService.getRegularStop(stopId);
    if (stop == null) {
      throw new EntityNotFoundException("StopPlace", stopId);
    }
    var stopTimesInPatterns = transitService
      .findStopTimesInPattern(
        stop,
        time.atZone(transitService.getTimeZone()).toInstant(),
        Duration.ofHours(2),
        10,
        ArrivalDeparture.BOTH,
        true
      )
      .stream()
      .flatMap(st -> st.times.stream())
      .map(CallAtStop::noWalking)
      .toList();

    return sort(numResults, stopTimesInPatterns);
  }

  public List<CallAtStop> findTripTimesOnDate(
    WgsCoordinate coordinate,
    Instant time,
    int numResults
  ) {
    var tripTimesOnDate = finder
      .findClosestStops(coordinate.asJtsCoordinate(), 1000)
      .stream()
      .flatMap(nearbyStop ->
        transitService
          .findStopTimesInPattern(
            nearbyStop.stop,
            time.plus(nearbyStop.duration()),
            Duration.ofHours(2),
            10,
            ArrivalDeparture.BOTH,
            true
          )
          .stream()
          .flatMap(tt -> tt.times.stream().map(t -> new CallAtStop(t, nearbyStop.duration())))
      )
      .toList();

    return sort(numResults, tripTimesOnDate);
  }

  private static List<CallAtStop> sort(int numResults, List<CallAtStop> stopTimesInPatterns) {
    return stopTimesInPatterns
      .stream()
      .sorted(Comparator.comparing(tt -> tt.tripTimeOnDate().departure()))
      .limit(numResults)
      .toList();
  }
}
