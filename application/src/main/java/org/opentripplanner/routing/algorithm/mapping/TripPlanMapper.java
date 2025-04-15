package org.opentripplanner.routing.algorithm.mapping;

import java.util.List;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.LocalizedString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.GenericLocation;
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
      from = placeFromGeoLocation(
        request != null ? request.from() : null,
        new LocalizedString("origin")
      );
      to = placeFromGeoLocation(
        request != null ? request.to() : null,
        new LocalizedString("destination")
      );
    } else {
      List<Leg> legs = itineraries.get(0).legs();
      from = legs.get(0).getFrom();
      to = legs.get(legs.size() - 1).getTo();
    }
    return new TripPlan(from, to, request.dateTime(), itineraries);
  }

  private static Place placeFromGeoLocation(GenericLocation location, I18NString defaultName) {
    if (location == null) {
      return Place.normal(null, null, defaultName);
    }

    return Place.normal(
      location.lat,
      location.lng,
      NonLocalizedString.ofNullableOrElse(location.label, defaultName)
    );
  }
}
