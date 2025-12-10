package org.opentripplanner.routing.algorithm.mapping;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.model.plan.leg.ViaLocationType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Maps locations to {@link ViaLocationType} based on their existence in the route request.
 */
public class ViaLocationTypeMapper {

  @Nullable
  public static ViaLocationType map(RouteRequest request, StopLocation stop) {
    return request
      .listViaLocations()
      .stream()
      .flatMap(viaLocation ->
        viaLocation
          .stopLocationIds()
          .stream()
          .map(stopId -> {
            // This might yield to false positive matches (stop location is visited multiple times
            // in an itinerary), but those cases should be quite rare.
            if (stopId.equals(stop.getId()) || stopId.equals(stop.getStationOrStopId())) {
              return viaLocation.isPassThroughLocation()
                ? ViaLocationType.PASS_THROUGH
                : ViaLocationType.VISIT;
            }
            return null;
          })
      )
      .filter(Objects::nonNull)
      .findFirst()
      .orElse(null);
  }

  @Nullable
  public static ViaLocationType map(RouteRequest request, TemporaryStreetLocation location) {
    var isViaLocation = request
      .listVisitViaLocations()
      .stream()
      .anyMatch(viaLocation ->
        viaLocation
          .coordinates()
          .stream()
          .anyMatch(coordinate -> coordinate.asJtsCoordinate().equals(location.getCoordinate()))
      );
    return isViaLocation ? ViaLocationType.VISIT : null;
  }
}
