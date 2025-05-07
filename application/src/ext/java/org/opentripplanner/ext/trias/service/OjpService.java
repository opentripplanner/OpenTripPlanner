package org.opentripplanner.ext.trias.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.transit.api.request.TripTimeOnDateRequest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.EntityNotFoundException;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.ArrivalDeparture;
import org.opentripplanner.transit.service.TransitService;

/**
 * Implements OJP specific business logic but delegates the bulk of the work to TransitService.
 */
public class OjpService {

  private final TransitService transitService;
  private final GraphFinder finder;
  /**
   * In order to avoid a DDOS attack we limit the number.
   */
  private final int MAX_DEPARTURES = 100;

  public OjpService(TransitService transitService, GraphFinder finder) {
    this.transitService = transitService;
    this.finder = finder;
  }

  /**
   * Find calls at stop at a specific time. These are useful for departure/arrival boards.
   */
  public List<CallAtStop> findCallsAtStop(FeedScopedId id, StopEventRequestParams params)
    throws EntityNotFoundException {
    Collection<StopLocation> stops;
    var stop = transitService.getStopLocation(id);
    if (stop != null) {
      stops = List.of(stop);
    } else {
      var station = transitService.getStopLocationsGroup(id);
      if (station == null) {
        throw new EntityNotFoundException("StopPointRef", id);
      }
      stops = station.getChildStops();
    }
    var calls = findCallsAtStop(stops, params);
    return sort(params.numDepartures, calls);
  }

  /**
   * Find calls at stop near a given coordinate at a specific time.
   * <p>
   * These are useful for departure/arrival boards.
   */
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

  /**
   * Extract the feed language from its ID.
   */
  Optional<String> resolveLanguage(String feedId) {
    return Optional.ofNullable(transitService.getFeedInfo(feedId)).map(FeedInfo::getLang);
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
    var builder = TripTimeOnDateRequest.of(stopLocations)
      .withTime(params.time)
      .withArrivalDeparture(params.arrivalDeparture)
      .withTimeWindow(params.timeWindow)
      .withNumberOfDepartures(params.numDepartures)
      .withExcludeAgencies(params.excludedAgencies)
      .withExcludeRoutes(params.excludedRoutes)
      .withExcludeModes(params.excludedModes)
      .withSortOrder(TripTimeOnDate.compareByScheduledDeparture());

    if (params.includesAgencies()) {
      builder.withIncludeAgencies(params.includedAgencies);
    }
    if (params.includesRoutes()) {
      builder.withIncludeRoutes(params.includedRoutes);
    }
    if (params.includesModes()) {
      builder.withIncludeModes(params.includedModes);
    }

    var request = builder.build();
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
  ) {
    public boolean includesAgencies() {
      return !includedAgencies.isEmpty();
    }
    public boolean includesRoutes() {
      return !includedRoutes().isEmpty();
    }
    public boolean includesModes() {
      return !includedModes.isEmpty();
    }
  }
}
