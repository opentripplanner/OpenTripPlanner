package org.opentripplanner.transit.raptor.speed_test.transit;

import org.opentripplanner.transit.raptor.speed_test.SpeedTestRequest;
import org.opentripplanner.transit.raptor.speed_test.api.model.Place;
import org.opentripplanner.transit.raptor.speed_test.api.model.TripPlan;


/**
 * Help SpeedTest to create a TripPlan and to add itineraries to it.
 */
public class TripPlanSupport {

    private TripPlanSupport() { }


    public static TripPlan createTripPlanForRequest(SpeedTestRequest request, ItinerarySet itineraries) {
        TripPlan tripPlan = createTripPlanForRequest(request);
        addItineraries(tripPlan, itineraries);
        return tripPlan;
    }

    public static TripPlan createTripPlanForRequest(SpeedTestRequest request) {
        TripPlan tripPlan = new TripPlan();
        tripPlan.date = request.getStartTimeAsDate();
        tripPlan.from = new Place(request.getFromLon(), request.getFromLat(), "Origin");
        tripPlan.to = new Place(request.getToLon(), request.getToLat(), "Destination");
        return tripPlan;
    }

    private static void addItineraries(TripPlan tripPlan, ItinerarySet itineraries) {
        for (SpeedTestItinerary it : itineraries) {
            tripPlan.addItinerary(it);
        }
        tripPlan.sort();
    }
}
