package org.opentripplanner.place.nearbystopfinder;

import java.util.Collection;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.collection.MinMap;

/**
 * Filters nearby stops based on flex trip availability.
 * <p>
 * This filter ensures that transfers are generated for stops used by flex trips. For each flex
 * trip, it keeps only the closest stop where the flex trip can board or alight (depending on
 * direction).
 */
class FlexTripNearbyStopFilter implements NearbyStopFilter {

  private final TransitService transitService;

  FlexTripNearbyStopFilter(TransitService transitService) {
    this.transitService = transitService;
  }

  @Override
  public boolean includeFromStop(FeedScopedId id, boolean reverseDirection) {
    return !transitService.getFlexIndex().getFlexTripsByStopId(id).isEmpty();
  }

  @Override
  public Collection<NearbyStop> filterToStops(
    Collection<NearbyStop> nearbyStops,
    boolean reverseDirection
  ) {
    MinMap<FlexTrip<?, ?>, NearbyStop> closestStopForFlexTrip = MinMap.ofNaturalOrder();
    for (var it : nearbyStops) {
      var stopId = it.stopId;
      var flexTrips = transitService.getFlexIndex().getFlexTripsByStopId(stopId);

      for (FlexTrip<?, ?> trip : flexTrips) {
        if (reverseDirection ? trip.isAlightingPossible(stopId) : trip.isBoardingPossible(stopId)) {
          closestStopForFlexTrip.putMin(trip, it);
        }
      }
    }
    return closestStopForFlexTrip.values();
  }
}
