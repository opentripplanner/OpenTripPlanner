package org.opentripplanner.model.plan;

import java.util.ArrayList;
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

    public List<Itinerary> itineraries = new ArrayList<>();

    public TripPlan() { }

    public TripPlan(Place from, Place to, Date date) {
        this.from = from;
        this.to = to;
        this.date = date;
    }
}
