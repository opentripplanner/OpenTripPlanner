package org.opentripplanner.routing.algorithm.mapping;

import java.util.Date;
import java.util.List;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.LocalizedString;
import org.opentripplanner.util.NonLocalizedString;

public class TripPlanMapper {

    /** This is a utility class with static method only. */
    private TripPlanMapper() { }

    public static TripPlan mapTripPlan(
            RoutingRequest request,
            List<Itinerary> itineraries
    ) {
        Place from;
        Place to;

        if(itineraries.isEmpty()) {
            from = placeFromGeoLocation(request.from, new LocalizedString("origin"));
            to = placeFromGeoLocation(request.to, new LocalizedString("destination"));
        }
        else {
            List<Leg> legs = itineraries.get(0).legs;
            from = legs.get(0).getFrom();
            to = legs.get(legs.size() - 1).getTo();
        }
        return new TripPlan(from, to, Date.from(request.getDateTime()), itineraries);
    }

    private static Place placeFromGeoLocation(GenericLocation location, I18NString defaultName) {
        return Place.normal(
                location.lat,
                location.lng,
                NonLocalizedString.ofNullableOrElse(location.label, defaultName)
        );
    }
}
