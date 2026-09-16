package org.opentripplanner.ext.taxizone;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.service.TransitService;

/**
 * Service for decorating and routing itineraries with taxi zone information.
 */
public interface TaxiZoneService {
  /**
   * Drops access candidates whose logical endpoints (the request origin and the stop) are not
   * covered by a common taxi zone provider.
   */
  Collection<NearbyStop> filterAccessNearbyStops(
    TransitService transitService,
    Collection<NearbyStop> nearbyStops,
    WgsCoordinate requestFrom
  );

  /**
   * Drops egress candidates whose logical endpoints (the stop and the request destination) are
   * not covered by a common taxi zone provider.
   */
  Collection<NearbyStop> filterEgressNearbyStops(
    TransitService transitService,
    Collection<NearbyStop> nearbyStops,
    WgsCoordinate requestTo
  );

  /**
   * Decorates the {@link TraverseMode#CAR} leg among an access or egress leg chain with taxi zone
   * information, looking up the zone using the given logical {@code pickup} and {@code dropoff}
   * coordinates (the request origin/destination and the stop), rather than the leg's own local
   * coordinates.
   */
  List<Leg> decorateAccessEgressLegs(List<Leg> legs, WgsCoordinate pickup, WgsCoordinate dropoff);

  /**
   * Produces direct (non-transit) taxi itineraries for the given request, using ordinary street
   * routing and decorating the result with taxi zone information.
   */
  List<Itinerary> routeDirect(
    TransitService transitService,
    RouteRequest request,
    LinkingContext linkingContext
  );
}
