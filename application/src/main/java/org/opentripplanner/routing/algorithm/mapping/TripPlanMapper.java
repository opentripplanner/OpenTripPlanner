package org.opentripplanner.routing.algorithm.mapping;

import java.util.List;
import org.opentripplanner.framework.i18n.LocalizedString;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.routing.api.request.RouteRequest;

public class TripPlanMapper {

  /** This is a utility class with static method only. */
  private TripPlanMapper() {}

  public static TripPlan mapTripPlan(RouteRequest request, List<Itinerary> itineraries) {
    Place from;
    Place to;

    if (itineraries.isEmpty()) {
      from = Place.forGenericLocation(request.from(), new LocalizedString("origin"));
      to = Place.forGenericLocation(request.to(), new LocalizedString("destination"));
    } else {
      List<Leg> legs = itineraries.getFirst().legs();
      from = legs.getFirst().from();
      to = legs.getLast().to();
    }
    return new TripPlan(from, to, request.dateTime(), itineraries);
  }
}
