package org.opentripplanner.ext.vdv;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.FeedInfo;
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

  public List<CallAtStop> findCallsAtStop(FeedScopedId stopId, StopEventRequestParams params)
    throws EntityNotFoundException {
    var stop = transitService.getRegularStop(stopId);
    if (stop == null) {
      throw new EntityNotFoundException("StopPlace", stopId);
    }
    return findCallsAtStop(stop, params);
  }

  public List<CallAtStop> findCallsAtStop(StopLocation stop, StopEventRequestParams params) {
    List<StopLocation> stopLocations = List.of(stop);
    var calls = findCallsAtStop(stopLocations, params);
    return sort(params.numDepartures, calls);
  }

  public List<CallAtStop> findCallsAtStop(WgsCoordinate coordinate, StopEventRequestParams params) {
    var calls = finder
      .findClosestStops(coordinate.asJtsCoordinate(), params.maximumWalkDistance)
      .stream()
      .flatMap(nearbyStop ->
        this.findCallsAtStop(nearbyStop.stop, params)
          .stream()
          .map(tt -> tt.withWalkTime(nearbyStop.duration()))
      )
      .toList();

    return sort(params.numDepartures(), calls);
  }

  private static List<CallAtStop> sort(int numResults, List<CallAtStop> stopTimesInPatterns) {
    return stopTimesInPatterns
      .stream()
      .sorted(Comparator.comparing(tt -> tt.tripTimeOnDate().departure()))
      .limit(numResults)
      .toList();
  }

  private List<CallAtStop> findCallsAtStop(
    List<StopLocation> stopLocations,
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
      .withIncludedAgencies(
        FilterValues.ofEmptyIsEverything("selectedAgencies", params.includedAgencies)
      )
      .withIncludedRoutes(FilterValues.ofEmptyIsEverything("selectedRoutes", params.includedRoutes))
      .withExcludedAgencies(
        FilterValues.ofEmptyIsEverything("excludedAgencies", params.excludedAgencies)
      )
      .withExcludedRoutes(FilterValues.ofEmptyIsEverything("excludedRoutes", params.excludedRoutes))
      .withIncludedModes(FilterValues.ofEmptyIsEverything("selectedModes", params.includedModes))
      .withExcludedModes(FilterValues.ofEmptyIsEverything("excludedModes", params.excludedModes))
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
