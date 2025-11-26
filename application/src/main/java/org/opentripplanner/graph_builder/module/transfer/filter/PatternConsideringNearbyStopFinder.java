package org.opentripplanner.graph_builder.module.transfer.filter;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.graph_builder.module.nearbystops.NearbyStopFinder;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.service.TransitService;

/**
 * A {@link NearbyStopFinder} that filters nearby stops based on trip patterns and flex trips.
 * <p>
 * This finder delegates to another NearbyStopFinder to find physically nearby stops, then filters
 * them to include only stops that:
 * <ul>
 *   <li>Are served by trip patterns where boarding/alighting is possible, OR</li>
 *   <li>Are served by flex trips (if FlexRouting is enabled), OR</li>
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

  public PatternConsideringNearbyStopFinder(
    TransitService transitService,
    NearbyStopFinder delegateNearbyStopFinder
  ) {
    var builder = CompositeNearbyStopFilter.of().add(new PatternNearbyStopFilter(transitService));

    if (OTPFeature.FlexRouting.isOn()) {
      builder.add(new FlexTripNearbyStopFilter(transitService));
    }
    this.filter = builder.build();
    this.delegateNearbyStopFinder = delegateNearbyStopFinder;
  }

  @Override
  public List<NearbyStop> findNearbyStops(
    Vertex vertex,
    RouteRequest routingRequest,
    StreetRequest streetRequest,
    boolean reverseDirection
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
      streetRequest,
      reverseDirection
    );

    // Remove transfersNotAllowed stops BEFORE we filter in Pattern and Flex Trips
    nearbyStops = removeTransferNotAllowedStops(nearbyStops);

    // Run TripPattern and FlexTrip filters
    var result = filter.filterToStops(nearbyStops, reverseDirection);

    return List.copyOf(result);
  }

  private static Collection<NearbyStop> removeTransferNotAllowedStops(
    Collection<NearbyStop> nearbyStops
  ) {
    return nearbyStops.stream().filter(s -> !s.stop.transfersNotAllowed()).toList();
  }
}
