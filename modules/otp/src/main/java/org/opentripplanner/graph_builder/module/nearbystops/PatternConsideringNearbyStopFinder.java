package org.opentripplanner.graph_builder.module.nearbystops;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitService;

public class PatternConsideringNearbyStopFinder implements NearbyStopFinder {

  private final NearbyStopFinder delegateNearbyStopFinder;
  private final TransitService transitService;

  public PatternConsideringNearbyStopFinder(
    TransitService transitService,
    NearbyStopFinder delegateNearbyStopFinder
  ) {
    this.transitService = transitService;
    this.delegateNearbyStopFinder = delegateNearbyStopFinder;
  }

  /**
   * Find all unique nearby stops that are the closest stop on some trip pattern or flex trip. Note
   * that the result will include the origin vertex if it is an instance of StopVertex. This is
   * intentional: we don't want to return the next stop down the line for trip patterns that pass
   * through the origin vertex. Taking the patterns into account reduces the number of transfers
   * significantly compared to simple traverse-duration-constrained all-to-all stop linkage.
   */
  @Override
  public List<NearbyStop> findNearbyStops(
    Vertex vertex,
    RouteRequest routingRequest,
    StreetRequest streetRequest,
    boolean reverseDirection
  ) {
    /* Track the closest stop on each pattern passing nearby. */
    MinMap<TripPattern, NearbyStop> closestStopForPattern = new MinMap<>();

    /* Track the closest stop on each flex trip nearby. */
    MinMap<FlexTrip<?, ?>, NearbyStop> closestStopForFlexTrip = new MinMap<>();

    /* Iterate over nearby stops via the street network or using straight-line distance. */
    for (NearbyStop nearbyStop : delegateNearbyStopFinder.findNearbyStops(
      vertex,
      routingRequest,
      streetRequest,
      reverseDirection
    )) {
      StopLocation ts1 = nearbyStop.stop;

      if (ts1 instanceof RegularStop) {
        /* Consider this destination stop as a candidate for every trip pattern passing through it. */
        for (TripPattern pattern : transitService.getPatternsForStop(ts1)) {
          if (
            reverseDirection
              ? pattern.canAlight(nearbyStop.stop)
              : pattern.canBoard(nearbyStop.stop)
          ) {
            closestStopForPattern.putMin(pattern, nearbyStop);
          }
        }
      }

      if (OTPFeature.FlexRouting.isOn()) {
        for (FlexTrip<?, ?> trip : transitService.getFlexIndex().getFlexTripsByStop(ts1)) {
          if (
            reverseDirection
              ? trip.isAlightingPossible(nearbyStop.stop)
              : trip.isBoardingPossible(nearbyStop.stop)
          ) {
            closestStopForFlexTrip.putMin(trip, nearbyStop);
          }
        }
      }
    }

    /* Make a transfer from the origin stop to each destination stop that was the closest stop on any pattern. */
    Set<NearbyStop> uniqueStops = new HashSet<>();
    uniqueStops.addAll(closestStopForFlexTrip.values());
    uniqueStops.addAll(closestStopForPattern.values());
    // TODO: don't convert to list
    return uniqueStops.stream().toList();
  }
}
