package org.opentripplanner.routing.algorithm.mapping;

import java.time.Instant;
import java.util.List;
import org.opentripplanner.framework.i18n.LocalizedString;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteRequestBuilder;

public class TripPlanMapper {

  /** This is a utility class with static method only. */
  private TripPlanMapper() {}

  /**
   * Build a TripPlan for a route request and the itineraries found for this request.
   */
  public static TripPlan mapTripPlan(RouteRequest request, List<Itinerary> itineraries) {
    return mapTripPlan(request.from(), request.to(), request.dateTime(), itineraries);
  }

  /**
   * Build a TripPlan for an invalid route request, based on the route request builder.
   * This is useful when the request cannot be built because of validation errors.
   * The resulting TripPlan contains an empty list of itineraries.
   */
  public static TripPlan mapEmptyTripPlan(RouteRequestBuilder builder) {
    return mapTripPlan(builder.from(), builder.to(), builder.dateTime(), List.of());
  }

  private static TripPlan mapTripPlan(
    GenericLocation fromLocation,
    GenericLocation toLocation,
    Instant dateTime,
    List<Itinerary> itineraries
  ) {
    Place from;
    Place to;

    if (itineraries.isEmpty()) {
      from = Place.forGenericLocation(fromLocation, new LocalizedString("origin"));
      to = Place.forGenericLocation(toLocation, new LocalizedString("destination"));
    } else {
      List<Leg> legs = itineraries.getFirst().legs();
      from = legs.getFirst().from();
      to = legs.getLast().to();
    }
    return new TripPlan(from, to, dateTime, itineraries);
  }
}
