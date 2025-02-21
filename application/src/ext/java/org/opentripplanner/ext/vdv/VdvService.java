package org.opentripplanner.ext.vdv;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.routing.stoptimes.ArrivalDeparture;
import org.opentripplanner.transit.api.request.TripTimeOnDateRequest;
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

  public List<CallAtStop> findTripTimesOnDate(
    FeedScopedId stopId,
    Instant time,
    ArrivalDeparture arrivalDeparture,
    Duration timeWindow,
    int numResults
  ) throws EntityNotFoundException {
    var stop = transitService.getRegularStop(stopId);
    if (stop == null) {
      throw new EntityNotFoundException("StopPlace", stopId);
    }
    var timesAtStops = List.of(new TripTimeOnDateRequest.TimeAtStop(stop, time));

    var request = TripTimeOnDateRequest
      .of(timesAtStops)
      .withArrivalDeparture(arrivalDeparture)
      .withTimeWindow(timeWindow)
      .withNumberOfDepartures(numResults)
      .build();
    var stopTimesInPatterns = transitService
      .findTripTimeOnDate(request)
      .stream()
      .map(CallAtStop::noWalking)
      .toList();

    return sort(numResults, stopTimesInPatterns);
  }

  public List<CallAtStop> findTripTimesOnDate(
    WgsCoordinate coordinate,
    Instant time,
    ArrivalDeparture arrivalDeparture,
    Duration timeWindow,
    int numResults
  ) {
    var tripTimesOnDate = finder
      .findClosestStops(coordinate.asJtsCoordinate(), 1000)
      .stream()
      .flatMap(nearbyStop ->
        this.findTripTimesOnDate(
            nearbyStop.stop.getId(),
            time.plus(nearbyStop.duration()),
            arrivalDeparture,
            timeWindow,
            numResults
          )
          .stream()
          .map(tt -> tt.withWalkTime(nearbyStop.duration()))
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
