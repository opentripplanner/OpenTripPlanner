package org.opentripplanner.ext.carpooling.filter;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;

/**
 * Combines multiple {@link CarpoolItineraryFilter}s using AND logic, applied after routing to the
 * fully computed carpool itineraries. Use {@link #defaults()} for the standard configuration.
 */
public class ItineraryPostFilters implements CarpoolItineraryFilter {

  private final List<CarpoolItineraryFilter> filters;

  public ItineraryPostFilters(List<CarpoolItineraryFilter> filters) {
    this.filters = filters;
  }

  public static ItineraryPostFilters defaults() {
    return new ItineraryPostFilters(
      List.of(new DepartAfterItineraryFilter(), new ArriveByItineraryFilter())
    );
  }

  @Override
  public boolean isValidItinerary(
    Itinerary itinerary,
    CarpoolingRequest request,
    Duration searchWindow
  ) {
    return filters
      .stream()
      .allMatch(filter -> filter.isValidItinerary(itinerary, request, searchWindow));
  }
}
