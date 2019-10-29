package org.opentripplanner.transit.raptor.speed_test.transit;

import com.conveyal.r5.profile.ProfileRequest;
import org.opentripplanner.transit.raptor.speed_test.api.model.Place;
import org.opentripplanner.transit.raptor.speed_test.api.model.TripPlan;

import java.time.ZoneId;
import java.util.Date;


/**
 * Help SpeedTest to create a TripPlan and to add itineraries to it.
 */
public class TripPlanSupport {

    private TripPlanSupport() { }


    public static TripPlan createTripPlanForRequest(ProfileRequest request, ItinerarySet itineraries) {
        TripPlan tripPlan = createTripPlanForRequest(request);
        addItineraries(tripPlan, itineraries);
        return tripPlan;
    }

    public static TripPlan createTripPlanForRequest(ProfileRequest request) {
        TripPlan tripPlan = new TripPlan();
        tripPlan.date = new Date(request.date.atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant().toEpochMilli() + request.fromTime * 1000);
        tripPlan.from = new Place(request.fromLon, request.fromLat, "Origin");
        tripPlan.to = new Place(request.toLon, request.toLat, "Destination");
        return tripPlan;
    }

    private static void addItineraries(TripPlan tripPlan, ItinerarySet itineraries) {
        for (SpeedTestItinerary it : itineraries) {
            tripPlan.addItinerary(it);
        }
        tripPlan.sort();
    }


}
