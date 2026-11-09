package org.opentripplanner.ext.taxi;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.service.TransitService;

/**
 * Service for decorating and routing itineraries with taxi provider information.
 */
public interface TaxiService {
  /**
   * Drops access/egress candidates whose logical endpoints (the request origin/destination and the
   * stop) are not covered by a common taxi provider.
   */
  Collection<NearbyStop> filterNearbyStops(
    TransitService transitService,
    Collection<NearbyStop> nearbyStops,
    AccessEgressType type,
    RouteRequest request
  );

  /**
   * Returns {@code true} if a single taxi provider covers both the {@code pickup} and the
   * {@code dropoff} coordinates. This is used to check if a taxi can be used for an access/egress
   * segment that doesn't end at a stop, for example the segment between the origin and a via
   * location.
   */
  boolean isCoveredByCommonProvider(WgsCoordinate pickup, WgsCoordinate dropoff);

  /**
   * Decorates the {@link TraverseMode#CAR} leg among an access or egress leg chain with taxi
   * provider information, looking up the provider using the given logical {@code pickup} and
   * {@code dropoff} coordinates (the request origin/destination and the stop), rather than the
   * leg's own local coordinates.
   */
  List<Leg> decorateAccessEgressLegs(List<Leg> legs, WgsCoordinate pickup, WgsCoordinate dropoff);

  /**
   * Produces direct (non-transit) taxi itineraries for the given request, using ordinary street
   * routing and decorating the result with taxi provider information.
   */
  List<Itinerary> routeDirect(
    TransitService transitService,
    RouteRequest request,
    LinkingContext linkingContext
  );
}
