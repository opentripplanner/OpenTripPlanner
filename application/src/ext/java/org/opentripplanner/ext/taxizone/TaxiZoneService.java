package org.opentripplanner.ext.taxizone;

import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.transit.service.TransitService;

/**
 * Service for decorating and routing itineraries with taxi zone information.
 */
public interface TaxiZoneService {
  /**
   * Decorates the driving-ish legs of the given itineraries with taxi zone information, replacing
   * them with {@link org.opentripplanner.ext.taxizone.model.TaxiZoneLeg}s. Itineraries whose
   * driving-ish legs have no matching taxi zone provider are removed from the result.
   */
  List<Itinerary> decorateAndFilter(List<Itinerary> itineraries);

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
