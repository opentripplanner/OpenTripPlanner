package org.opentripplanner.place.nearbystopfinder;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.ext.flex.FlexIndex;
import org.opentripplanner.place.NearbyStopFinder;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.SiteRepository;

/**
 * A {@link NearbyStopFinder} that filters nearby stops based on trip patterns and flex trips.
 * <p>
 * This finder delegates to another NearbyStopFinder to find physically nearby stops, then filters
 * them to include only stops that:
 * <ul>
 *   <li>Are served by trip patterns where boarding/alighting is possible, OR</li>
 *   <li>Are served by flex trips (if a flex index is given), OR</li>
 *   <li>Are sometimes used by real-time trips (if IncludeStopsUsedRealTimeInTransfers is enabled)</li>
 * </ul>
 * <p>
 * For each trip pattern, only the closest stop is included to reduce the number of transfers
 * generated. This significantly improves transfer generation performance by limiting transfers to
 * the most relevant stops.
 */
public class PatternConsideringNearbyStopFinder implements NearbyStopFinder {

  private final NearbyStopFilter filter;

  private final NearbyStopFinder delegateNearbyStopFinder;

  private final SiteRepository siteRepository;

  /**
   * @param patternsForStop the scheduled trip patterns visiting a stop
   * @param flexIndex the flex trips to consider, or {@code null} to ignore flex trips (when flex
   *                  routing is disabled)
   */
  public PatternConsideringNearbyStopFinder(
    SiteRepository siteRepository,
    Function<StopLocation, Collection<TripPattern>> patternsForStop,
    @Nullable FlexIndex flexIndex,
    NearbyStopFinder delegateNearbyStopFinder
  ) {
    this.siteRepository = siteRepository;
    var builder = CompositeNearbyStopFilter.of().add(
      new PatternNearbyStopFilter(siteRepository, patternsForStop)
    );

    if (flexIndex != null) {
      builder.add(new FlexTripNearbyStopFilter(flexIndex));
    }
    this.filter = builder.build();
    this.delegateNearbyStopFinder = delegateNearbyStopFinder;
  }

  @Override
  public List<NearbyStop> findNearbyStops(Coordinate coordinate, double radiusMeters) {
    throw new UnsupportedOperationException(
      "This method is currently unsupported for this implementation."
    );
  }

  @Override
  public List<NearbyStop> findNearbyStops(
    Vertex vertex,
    RouteRequest routingRequest,
    StreetMode streetMode,
    boolean reverseDirection,
    Duration durationLimit,
    int maxStopCount
  ) {
    if (!(vertex instanceof TransitStopVertex stopVertex)) {
      throw new IllegalArgumentException(
        "Transfers can only be created between stops. Vertex: " + vertex
      );
    }

    // Check if the from stop can be used in a transfer
    if (!filter.includeFromStop(stopVertex.getId(), reverseDirection)) {
      return List.of();
    }

    // fetch nearby stops via the street network or using straight-line distance.
    var nearbyStops = delegateNearbyStopFinder.findNearbyStops(
      vertex,
      routingRequest,
      streetMode,
      reverseDirection,
      durationLimit,
      maxStopCount
    );

    // Remove transfersNotAllowed stops BEFORE we filter in Pattern and Flex Trips
    nearbyStops = removeTransferNotAllowedStops(nearbyStops);

    // Run TripPattern and FlexTrip filters
    var result = filter.filterToStops(nearbyStops, reverseDirection);

    return List.copyOf(result);
  }

  private Collection<NearbyStop> removeTransferNotAllowedStops(Collection<NearbyStop> nearbyStops) {
    return nearbyStops
      .stream()
      .filter(s -> !siteRepository.getStopLocation(s.stopId()).transfersNotAllowed())
      .toList();
  }
}
