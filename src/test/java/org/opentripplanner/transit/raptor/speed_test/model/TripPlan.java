package org.opentripplanner.transit.raptor.speed_test.model;

import org.opentripplanner.transit.raptor.api.response.RaptorResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * A TripPlan is a set of ways to get from point A to point B at time T.
 */
public class TripPlan {

    /**  The time and date of travel */
    public Date date = null;
    
    /** The origin */
    public Place from = null;
    
    /** The destination */
    public Place to = null;

    /** The raptor response (for reference use) */
    public RaptorResponse<?> response = null;

    /** A list of possible itineraries */
    private List<Itinerary> itineraries = new ArrayList<Itinerary>();

    public TripPlan(
            Date date,
            Place from,
            Place to,
            RaptorResponse<?> response,
            Iterable<? extends Itinerary> itineraries
    ) {
        this.date = date;
        this.from = from;
        this.to = to;
        this.response = response;
        for (Itinerary it : itineraries) {
            addItinerary(it);
        }
        sort();
    }

    public Collection<Itinerary> getItineraries() {
        return itineraries;
    }

    public void addItinerary(Itinerary itinerary) {
        if(itinerary != null) {
            this.itineraries.add(itinerary);
        }
    }

    public void sort() {
        itineraries.sort(Comparator.comparing(o -> o.endTime));
    }

}
