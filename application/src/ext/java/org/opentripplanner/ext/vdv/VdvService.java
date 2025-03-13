package org.opentripplanner.ext.vdv;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.api.request.TripTimeOnDateRequest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.EntityNotFoundException;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.ArrivalDeparture;
import org.opentripplanner.transit.service.TransitService;

public class VdvService {

  private final TransitService transitService;
  private final GraphFinder finder;
  /**
   * In order to avoid a DDOS attack we limit the number.
   */
  private final int MAX_DEPARTURES = 100;

  public VdvService(TransitService transitService, GraphFinder finder) {
    this.transitService = transitService;
    this.finder = finder;
  }

  public Optional<String> resolveLanguage(String feedId) {
    return Optional.ofNullable(transitService.getFeedInfo(feedId)).map(FeedInfo::getLang);
  }

  public List<CallAtStop> findCallsAtStop(FeedScopedId id, StopEventRequestParams params)
    throws EntityNotFoundException {
    Collection<StopLocation> stops;
    var stop = transitService.getStopLocation(id);
    if (stop != null) {
      stops = List.of(stop);
    } else {
      var station = transitService.getStopLocationsGroup(id);
      if (station == null) {
        throw new EntityNotFoundException("Stop entity", id);
      }
      stops = station.getChildStops();
    }
    var calls = findCallsAtStop(stops, params);
    return sort(params.numDepartures, calls);
  }

  public List<CallAtStop> findCallsAtStop(WgsCoordinate coordinate, StopEventRequestParams params) {
    var calls = finder
      .findClosestStops(coordinate.asJtsCoordinate(), params.maximumWalkDistance)
      .stream()
      .flatMap(nearbyStop -> {
        List<StopLocation> stopLocations = List.of(nearbyStop.stop);
        var calls1 = findCallsAtStop(stopLocations, params);
        return sort(params.numDepartures, calls1)
          .stream()
          .map(tt -> tt.withWalkTime(nearbyStop.duration()));
      })
      .toList();

    return sort(params.numDepartures(), calls);
  }

  private static List<CallAtStop> sort(int numResults, List<CallAtStop> stopTimesInPatterns) {
    return stopTimesInPatterns
      .stream()
      .sorted(CallAtStop.compareByScheduledDeparture())
      .limit(numResults)
      .toList();
  }

  private List<CallAtStop> findCallsAtStop(
    Collection<StopLocation> stopLocations,
    StopEventRequestParams params
  ) {
    if (params.numDepartures > MAX_DEPARTURES) {
      throw new IllegalArgumentException(
        "Number of departures must be less than " + MAX_DEPARTURES
      );
    }
    var request = TripTimeOnDateRequest.of(stopLocations)
      .withTime(params.time)
      .withArrivalDeparture(params.arrivalDeparture)
      .withTimeWindow(params.timeWindow)
      .withNumberOfDepartures(params.numDepartures)
      .withIncludedAgencies(params.includedAgencies)
      .withIncludedRoutes(params.includedRoutes)
      .withExcludedAgencies(params.excludedAgencies)
      .withExcludedRoutes(params.excludedRoutes)
      .withIncludedModes(params.includedModes)
      .withExcludedModes(params.excludedModes)
      .withSortOrder(TripTimeOnDate.compareByScheduledDeparture())
      .build();
    return transitService.findTripTimesOnDate(request).stream().map(CallAtStop::noWalking).toList();
  }

  public record StopEventRequestParams(
    Instant time,
    ArrivalDeparture arrivalDeparture,
    Duration timeWindow,
    int maximumWalkDistance,
    int numDepartures,
    Set<FeedScopedId> includedAgencies,
    Set<FeedScopedId> includedRoutes,
    Set<FeedScopedId> excludedAgencies,
    Set<FeedScopedId> excludedRoutes,
    Set<TransitMode> includedModes,
    Set<TransitMode> excludedModes
  ) {}
}
